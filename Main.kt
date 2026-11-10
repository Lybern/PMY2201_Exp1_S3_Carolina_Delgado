import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * ============================================================================
 * ASIGNATURA: Desarrollo de Aplicaciones Móviles I (PMY2201)
 * EVALUACIÓN FORMATIVA: Semana 2 - "Aplicando utilidades de Kotlin"
 * ALUMNO / PROYECTO: Sistema de Gestión de Pizzería Kotlin (Refactorización POO)
 * ============================================================================
 */

// ============================================================================
// PASO 3: PROGRAMACIÓN ORIENTADA A OBJETOS (POO) - HERENCIA Y CLASES
// ============================================================================

/**
 * Clase base que modela a un usuario genérico del sistema.
 * Utiliza 'open' para permitir que otras clases hereden de ella.
 */
open class Usuario(
    val nombre: String,
    val contacto: String
) {
    open fun obtenerIdentificacion(): String = "$nombre ($contacto)"
}

/**
 * Subclase Cliente que hereda de Usuario y añade correo y dirección.
 */
class Cliente(
    nombre: String,
    contacto: String,
    val email: String?,
    val direccion: String
) : Usuario(nombre, contacto) {

    fun obtenerFichaCliente(): String =
        "Cliente: $nombre | Teléfono: $contacto | Email: ${email ?: "correo@invalido.com"} | Dirección: $direccion"

    /**
     * PASO 4 (Declaraciones de Desestructuración):
     * Permite desestructurar directamente un objeto Cliente en sus componentes clave:
     * val (nombre, correo, telefono) = cliente
     */
    operator fun component1(): String = nombre
    operator fun component2(): String = email ?: "correo@invalido.com"
    operator fun component3(): String = contacto
    operator fun component4(): String = direccion

    /**
     * PASO 5 (Evaluación de Igualdad equals / hashCode):
     * Dos clientes se consideran iguales si coinciden su nombre y su correo electrónico.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cliente) return false
        return this.nombre.equals(other.nombre, ignoreCase = true) &&
               (this.email ?: "").equals(other.email ?: "", ignoreCase = true)
    }

    override fun hashCode(): Int {
        var result = nombre.lowercase().hashCode()
        result = 31 * result + (email?.lowercase()?.hashCode() ?: 0)
        return result
    }
}

/**
 * Subclase Repartidor que hereda de Usuario y añade disponibilidad y distancia.
 */
class Repartidor(
    nombre: String,
    contacto: String,
    val distanciaKm: Double,
    var disponible: Boolean = true
) : Usuario(nombre, contacto) {

    /**
     * Método específico que formaliza la entrega y actualiza el pedido.
     */
    fun entregarPedido(pedido: Pedido) {
        require(pedido.repartidor === this) { "El pedido está asignado a otro repartidor." }
        pedido.cambiarEstado("Entregado")
        this.disponible = true
        println("🚴 [Repartidor $nombre]: Ha entregado con éxito el pedido #${pedido.id.take(8)} en ${pedido.cliente.direccion}.")
    }
}

// ============================================================================
// PASO 2: ANOTACIONES PERSONALIZADAS Y METAPROGRAMACIÓN (REFLECTION)
// ============================================================================

/**
 * PASO 2 (Anotaciones):
 * Anotación personalizada @Promocionable para identificar pizzas o combos que califican
 * para promociones especiales.
 * Configurada con @Target(AnnotationTarget.CLASS) y @Retention(AnnotationRetention.RUNTIME)
 * para permitir su análisis dinámico mediante Reflection en tiempo de ejecución.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Promocionable(
    val descripcion: String = "Producto o combo elegible para promociones especiales",
    val descuentoAdicional: Double = 0.05
)

/**
 * Modela un producto de la pizzería con sus ingredientes, precio y categoría.
 * PASO 2 & PASO 3: Se añade soporte de categoría ("Pizza", "Combo", "Bebida", "Ingrediente")
 * y se habilita como clase 'open' para permitir productos promocionales anotados.
 */
open class Producto(
    val nombre: String,
    val precio: Double,
    val ingredientes: List<String>,
    val categoria: String = "Pizza"
) {
    init {
        require(precio.isFinite() && precio > 0) { "El precio debe ser positivo y finito." }
    }

    /**
     * Retorna la descripción completa del producto con sus ingredientes, precio y categoría.
     * PASO 2: Detecta dinámicamente mediante Reflection si la clase está anotada con @Promocionable.
     */
    open fun detallarProducto(): String {
        val precioFormateado = String.format(Locale.US, "%.2f", precio)
        val esPromo = esProductoPromocionable(this)
        val etiquetaPromo = if (esPromo) " ⭐ [PROMOCIONABLE]" else ""
        return "[$categoria] $nombre - $$precioFormateado$etiquetaPromo | Ingredientes: ${ingredientes.joinToString(", ")}"
    }

    /**
     * PASO 3 (Operator Overloading '=='):
     * Sobrecarga el operador '==' (implementando equals) para comparar si dos objetos
     * representan el mismo producto basándose en su nombre y categoría (pizza, bebida, combo, ingrediente),
     * cumpliendo estrictamente con la pauta de evaluación sumativa.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Producto) return false
        return this.nombre.equals(other.nombre, ignoreCase = true) &&
               this.categoria.equals(other.categoria, ignoreCase = true)
    }

    /**
     * PASO 5 (Evaluación de Igualdad):
     * Consistente con equals(), genera el hash code combinando nombre y categoría.
     */
    override fun hashCode(): Int {
        var result = nombre.lowercase().hashCode()
        result = 31 * result + categoria.lowercase().hashCode()
        return result
    }
}

/**
 * PASO 2 (Anotaciones):
 * Subclase ComboFamiliar anotada con @Promocionable (10% de descuento adicional).
 */
@Promocionable(
    descripcion = "Combo Familiar Duoc UC: 2 pizzas + bebida con 10% de descuento adicional",
    descuentoAdicional = 0.10
)
open class ComboFamiliar(
    nombre: String = "Combo Familiar Duoc UC",
    precio: Double = 16990.0,
    ingredientes: List<String> = listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni", "Bebida Cola")
) : Producto(nombre, precio, ingredientes, "Combo")

/**
 * PASO 2 (Anotaciones):
 * Subclase PizzaPromocional anotada con @Promocionable (5% de descuento adicional).
 */
@Promocionable(
    descripcion = "Pizza Promo Suprema de temporada con 5% de descuento adicional",
    descuentoAdicional = 0.05
)
open class PizzaPromocional(
    nombre: String = "Pizza Promo Suprema",
    precio: Double = 10500.0,
    ingredientes: List<String> = listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni", "Champiñones")
) : Producto(nombre, precio, ingredientes, "Pizza")

/**
 * PASO 2 (Reflection):
 * Verifica dinámicamente en tiempo de ejecución si un producto o su clase está anotado con @Promocionable.
 */
fun esProductoPromocionable(producto: Producto): Boolean {
    return producto::class.java.isAnnotationPresent(Promocionable::class.java)
}

/**
 * PASO 2 (Reflection):
 * Recupera dinámicamente la instancia de la anotación @Promocionable para acceder a sus atributos.
 */
fun obtenerAnotacionPromocionable(producto: Producto): Promocionable? {
    return producto::class.java.getAnnotation(Promocionable::class.java)
}

/**
 * PASO 2 (Reflection):
 * Analiza e inspecciona dinámicamente mediante Reflection cualquier clase del dominio
 * (Cliente, Pedido, Producto, etc.), listando sus propiedades/atributos y métodos declarados.
 * Cumple con el requerimiento de análisis dinámico de entidades clave.
 */
fun inspeccionarEntidadConReflection(clase: Class<*>) {
    println("\n============================================================")
    println("🔬 [REFLECTION] INSPECCIÓN DINÁMICA DE LA CLASE: ${clase.simpleName}")
    println("============================================================")

    // 1. Verificación de anotaciones presentes a nivel de clase
    val anotaciones = clase.annotations
    println("🏷️  Anotaciones presentes (${anotaciones.size}):")
    if (anotaciones.isEmpty()) {
        println("   (Sin anotaciones directas en la clase)")
    } else {
        anotaciones.forEach { anot ->
            println("   * @${anot.annotationClass.simpleName} -> $anot")
        }
    }

    // 2. Propiedades / Campos declarados
    val campos = clase.declaredFields
    println("\n📋 Atributos / Propiedades declaradas (${campos.size}):")
    campos.forEach { campo ->
        campo.isAccessible = true
        println("   - ${campo.name}: ${campo.type.simpleName}")
    }

    // 3. Métodos declarados
    val metodos = clase.declaredMethods
    println("\n⚙️  Métodos y funciones declaradas (${metodos.size}):")
    metodos.forEach { metodo ->
        val params = metodo.parameterTypes.joinToString(", ") { it.simpleName }
        println("   - ${metodo.name}($params): ${metodo.returnType.simpleName}")
    }
    println("============================================================")
}

// ============================================================================
// PASO 1 (RANGES): DEFINICIÓN DE RANGOS DE PROMOCIONES Y CANTIDADES
// ============================================================================

val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * PASO 1 (Ranges): Rango de fechas para período de promociones especiales.
 * Se define un intervalo cerrado [FECHA_INICIO_PROMO .. FECHA_FIN_PROMO].
 */
val FECHA_INICIO_PROMO: LocalDate = LocalDate.now().minusDays(3)
val FECHA_FIN_PROMO: LocalDate = LocalDate.now().plusDays(10)
val PERIODO_PROMOCION: ClosedRange<LocalDate> = FECHA_INICIO_PROMO..FECHA_FIN_PROMO

/**
 * PASO 1 (Ranges): Rango permitido de unidades para pizzas e insumos (1..100).
 */
val RANGO_CANTIDAD_PERMITIDA: IntRange = 1..100

/**
 * Modela un pedido completo, encapsulando el cálculo de montos y estados.
 */
class Pedido(
    val id: String = UUID.randomUUID().toString(),
    val cliente: Cliente,
    val productos: List<Producto>,
    val notas: String? = null,
    val fecha: LocalDate = LocalDate.now(),
    var repartidor: Repartidor? = null
) {
    var estado = "En preparación"
        private set
    var subtotal: Double = 0.0
        private set
    var descuentoPromoFechas: Double = 0.0
        private set
    var descuentoVolumen: Double = 0.0
        private set
    var descuentoAnotacionPromo: Double = 0.0
        private set
    var descuento: Double = 0.0
        private set
    var impuestos: Double = 0.0
        private set
    var total: Double = 0.0
        private set

    init {
        require(productos.isNotEmpty()) { "Agregue al menos una pizza." }
        calcularTotal()
    }

    /**
     * PASO 1 (Ranges) & PASO 2 (Anotaciones & Reflection):
     * - Verifica si la fecha del pedido se encuentra dentro del rango de promociones (15% descuento).
     * - Identifica dinámicamente mediante Reflection productos anotados con @Promocionable y aplica su descuento extra.
     * - Aplica un 10% adicional si el cliente ordena más de 3 pizzas.
     */
    fun calcularTotal() {
        subtotal = productos.sumOf { it.precio }

        // PASO 1 (Ranges): Evaluación con operador 'in' dentro del rango de fechas
        val enRangoPromo = fecha in PERIODO_PROMOCION
        descuentoPromoFechas = if (enRangoPromo) subtotal * 0.15 else 0.0

        // PASO 2 (Anotaciones & Reflection): Identificación dinámica de productos con @Promocionable
        val prodsPromo = productos.filter { esProductoPromocionable(it) }
        descuentoAnotacionPromo = prodsPromo.sumOf { prod ->
            val meta = obtenerAnotacionPromocionable(prod)
            val porcentaje = meta?.descuentoAdicional ?: 0.05
            prod.precio * porcentaje
        }

        // Descuento por volumen (más de 3 productos)
        descuentoVolumen = if (productos.size > 3) subtotal * 0.10 else 0.0

        descuento = descuentoPromoFechas + descuentoVolumen + descuentoAnotacionPromo
        val baseImponible = (subtotal - descuento).coerceAtLeast(0.0)

        // Impuesto (IVA estándar 19%)
        impuestos = baseImponible * 0.19
        total = baseImponible + impuestos
    }

    /**
     * PASO 3 (Operator Overloading '+'):
     * Sobrecarga del operador '+' en la clase Pedido para combinar dos pedidos en uno solo.
     * Fusiona las listas de productos de ambos pedidos, combina las notas especiales
     * y recalcula automáticamente subtotal, descuentos (promoción en rango de fechas,
     * volumen > 3 y productos @Promocionable) y el total a pagar.
     *
     * Uso: val pedidoCombinado = pedido1 + pedido2
     */
    operator fun plus(otro: Pedido): Pedido {
        val productosCombinados = this.productos + otro.productos
        val notasCombinadas = when {
            this.notas != null && otro.notas != null -> "${this.notas} | ${otro.notas}"
            this.notas != null -> this.notas
            otro.notas != null -> otro.notas
            else -> null
        }
        val pedidoNuevo = Pedido(
            cliente = this.cliente,
            productos = productosCombinados,
            notas = notasCombinadas,
            fecha = if (this.fecha >= otro.fecha) this.fecha else otro.fecha
        )
        return pedidoNuevo
    }

    fun cambiarEstado(nuevoEstado: String) {
        val siguiente = when (estado) {
            "En preparación" -> "Listo"
            "Listo" -> "En camino"
            "En camino" -> "Entregado"
            else -> null
        }
        require(nuevoEstado == siguiente) { "Transición de estado inválida." }
        estado = nuevoEstado
    }

    /**
     * PASO 4 (Declaraciones de Desestructuración):
     * Permite la desestructuración de un objeto Pedido para extraer directamente
     * el cliente, los productos incluidos y el total del pedido en variables individuales:
     * val (cliente, productos, total) = pedido
     */
    operator fun component1(): Cliente = cliente
    operator fun component2(): List<Producto> = productos
    operator fun component3(): Double = total
    operator fun component4(): String = id
    operator fun component5(): String = estado

    /**
     * PASO 5 y 6: Presentación del resumen detallado y ordenado del pedido.
     * Utiliza plantillas de strings ($variable y ${expresión}) y operadores seguros.
     */
    fun generarResumen(): String {
        val detalleItems = productos
            .groupBy { it.nombre }
            .map { (nombre, items) ->
                val cant = items.size
                val unitario = items.first().precio
                val totalItem = unitario * cant
                "      - $nombre x$cant (Unitario: $${String.format(Locale.US, "%.2f", unitario)}) = $${String.format(Locale.US, "%.2f", totalItem)}"
            }
            .joinToString("\n")

        // Retroalimentación docente: .takeIf { it.isNotBlank() } para manejar cadenas en blanco correctamente
        val notasFormateadas = notas?.takeIf { it.isNotBlank() } ?: "Sin notas especiales"

        // PASO 4: Operador seguro (?.) para datos del repartidor
        val repartidorInfo = repartidor?.let { "${it.nombre} (${it.distanciaKm} km de distancia)" }
            ?: "Pendiente de asignación"

        val descPromoFechasTexto = if (descuentoPromoFechas > 0.0) {
            "\n   - Descuento Rango Fechas (15%): -$${String.format(Locale.US, "%.2f", descuentoPromoFechas)} [Promoción activa: ${FECHA_INICIO_PROMO.format(FORMATO_FECHA)} al ${FECHA_FIN_PROMO.format(FORMATO_FECHA)}]"
        } else ""

        val descAnotacionTexto = if (descuentoAnotacionPromo > 0.0) {
            "\n   - Descuento @Promocionable:      -$${String.format(Locale.US, "%.2f", descuentoAnotacionPromo)} (Identificado dinámicamente con Reflection)"
        } else ""

        val descVolumenTexto = if (descuentoVolumen > 0.0) {
            "\n   - Descuento por Volumen (10%):  -$${String.format(Locale.US, "%.2f", descuentoVolumen)} (>3 unidades)"
        } else ""

        val prodsPromo = productos.filter { esProductoPromocionable(it) }.distinctBy { it.nombre }
        val seccionPromos = if (prodsPromo.isNotEmpty()) {
            "\n------------------------------------------------------------\n" +
            "⭐ PRODUCTOS PROMOCIONABLES DETECTADOS (@Promocionable):\n" +
            prodsPromo.joinToString("\n") { prod ->
                val meta = obtenerAnotacionPromocionable(prod)
                val extraPct = (meta?.descuentoAdicional ?: 0.05) * 100
                "   * ${prod.nombre} -> Descuento especial: ${String.format(Locale.US, "%.1f", extraPct)}% [${meta?.descripcion ?: ""}]"
            }
        } else ""

        return """
        ============================================================
        🧾 RESUMEN DEL PEDIDO #${id.take(8)} | Fecha: ${fecha.format(FORMATO_FECHA)}
        ============================================================
        👤 DATOS DEL CLIENTE:
           - Nombre:    ${cliente.nombre}
           - Teléfono:  ${cliente.contacto}
           - Correo:    ${cliente.email ?: "correo@invalido.com"}
           - Dirección: ${cliente.direccion}
        ------------------------------------------------------------
        🍕 PRODUCTOS EN EL PEDIDO (${productos.size} unidades):
        ${detalleItems.replace("\n", "\n        ")}$seccionPromos
        ------------------------------------------------------------
        📝 NOTAS DEL PEDIDO:    $notasFormateadas
        🛵 REPARTIDOR ASIGNADO: $repartidorInfo
        📦 ESTADO DEL PEDIDO:   $estado
        ------------------------------------------------------------
        💵 DESGLOSE ECONÓMICO:
           - Subtotal:                     $${String.format(Locale.US, "%.2f", subtotal)}$descPromoFechasTexto$descAnotacionTexto$descVolumenTexto
           - Total Descuentos:            -$${String.format(Locale.US, "%.2f", descuento)}
           - Impuestos (IVA 19%):          $${String.format(Locale.US, "%.2f", impuestos)}
           - TOTAL A PAGAR:                $${String.format(Locale.US, "%.2f", total)}
        ============================================================
        """.trimIndent()
    }
}

// ============================================================================
// PASO 1 Y PASO 4: VALIDACIONES DE STRINGS Y MANEJO DE ERRORES
// ============================================================================

// ============================================================================
// PASO 1: REGEX (EXPRESIONES REGULARES) Y RANGES (RANGOS)
// ============================================================================

/**
 * PASO 1 (Regex - Validación de Correo):
 * Valida que el correo ingresado cumpla con el formato estándar (nombre@dominio.com).
 */
fun esEmailValido(emailInput: String): Boolean {
    val regex = Regex("""^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$""")
    return regex.matches(emailInput.trim())
}

/**
 * PASO 1 (Regex - Reingreso interactivo de Correo):
 * Valida el correo electrónico asegurándose de que sigue el formato estándar (nombre@dominio.com).
 * Si el formato es incorrecto, muestra un mensaje de error claro y solicita al usuario que lo reingrese
 * en un bucle continuo hasta que sea válido (requerimiento explícito del Paso 1).
 */
fun leerEmailObligatorio(mensaje: String = "Correo electrónico (ej. nombre@dominio.com): "): String {
    while (true) {
        print(mensaje)
        val entrada = leer().trim()
        if (esEmailValido(entrada)) {
            return entrada
        } else {
            println("❌ [Error Formato Email]: '$entrada' no es un correo válido (formato esperado: nombre@dominio.com). Intente nuevamente.")
        }
    }
}

/**
 * PASO 1 (Regex - Formateo Uniforme de Teléfono):
 * Procesa números de teléfono de clientes y los formatea mediante expresiones regulares
 * y grupos de captura en el estilo uniforme internacional: +XX (XXX) XXX-XXXX.
 */
fun formatearTelefono(telefonoInput: String): String {
    val digitos = telefonoInput.replace(Regex("[^0-9]"), "")
    // Normalizamos a 12 dígitos para ajustarse a +XX (XXX) XXX-XXXX
    val digitos12 = when {
        digitos.length == 9 -> "56" + "9" + digitos // Si ingresan 9 dígitos tipo 912345678 -> +56 (991) 234-5678
        digitos.length == 11 -> digitos.substring(0, 2) + "9" + digitos.substring(2) // 11 dígitos tipo 56912345678
        digitos.length == 12 -> digitos
        digitos.length > 12 -> digitos.take(12)
        else -> digitos.padStart(12, '0')
    }
    val regex = Regex("""^(\d{2})(\d{3})(\d{3})(\d{4})$""")
    return regex.replace(digitos12) { matchResult ->
        val pais = matchResult.groupValues[1]
        val area = matchResult.groupValues[2]
        val prefijo = matchResult.groupValues[3]
        val linea = matchResult.groupValues[4]
        "+$pais ($area) $prefijo-$linea"
    }
}

/**
 * PASO 1 (Regex - Validación interactiva de Teléfono):
 * Solicita el teléfono y aplica el formateo uniforme con Regex.
 */
fun leerTelefonoValido(mensaje: String = "Teléfono de contacto: "): String {
    while (true) {
        print(mensaje)
        val entrada = leer().trim()
        val digitos = entrada.replace(Regex("[^0-9]"), "")
        if (digitos.length in 8..15) {
            val formateado = formatearTelefono(entrada)
            println("📞 Teléfono normalizado con Regex: $formateado")
            return formateado
        } else {
            println("❌ [Error Formato Teléfono]: '$entrada' es inválido. Debe contener entre 8 y 15 dígitos. Intente nuevamente.")
        }
    }
}

/**
 * PASO 1 (Ranges - Validación de Cantidades de Productos/Insumos 1..100):
 * Evalúa si el número ingresado de productos (por ejemplo cantidad de pizzas o ingredientes adicionales)
 * cae dentro del rango permitido (1-100). Si no, muestra un mensaje de error claro y solicita reingresar.
 */
fun leerCantidadEnRango(
    mensaje: String = "Cantidad (1 a 100): ",
    rango: IntRange = RANGO_CANTIDAD_PERMITIDA
): Int {
    while (true) {
        print(mensaje)
        val linea = leer().trim()
        try {
            val valor = linea.toInt()
            if (valor in rango) {
                return valor
            } else {
                println("❌ [Error Rango de Cantidad]: El valor $valor está fuera del rango permitido (${rango.first} a ${rango.last}). Ingrese un valor válido.")
            }
        } catch (e: NumberFormatException) {
            println("❌ [Error Entrada]: Ingrese un número entero válido.")
        }
    }
}

// Funciones de compatibilidad hacia atrás
fun validarTelefono(telefonoInput: String): String = formatearTelefono(telefonoInput)
fun validarEmail(emailInput: String): String? = if (esEmailValido(emailInput)) emailInput.trim() else null

/**
 * PASO 4 (Errores):
 * Utiliza try-catch para capturar enteros válidos mayores o iguales a un mínimo.
 */
private class FinDeEntrada : RuntimeException()

private fun leer(): String = readlnOrNull()?.trim() ?: throw FinDeEntrada()

fun leerEntero(mensaje: String, minimo: Int = 0, maximo: Int = Int.MAX_VALUE): Int {
    while (true) {
        print(mensaje)
        val linea = leer()
        try {
            if (linea.isEmpty()) {
                throw IllegalArgumentException("La entrada no puede estar vacía.")
            }
            val numero = linea.toInt()
            if (numero !in minimo..maximo) {
                throw IllegalArgumentException("Ingrese un número entre $minimo y $maximo.")
            }
            return numero
        } catch (e: NumberFormatException) {
            println("❌ Error: Se esperaba un número entero válido. Intente nuevamente.")
        } catch (e: IllegalArgumentException) {
            println("❌ Error: ${e.message} Intente nuevamente.")
        }
    }
}

/**
 * PASO 4 (Errores):
 * Utiliza try-catch para evitar campos obligatorios vacíos.
 */
fun leerTextoObligatorio(mensaje: String): String {
    while (true) {
        print(mensaje)
        val linea = leer()
        try {
            if (linea.isEmpty()) {
                throw IllegalArgumentException("El campo solicitado es obligatorio.")
            }
            return linea
        } catch (e: IllegalArgumentException) {
            println("❌ Error: ${e.message} Intente de nuevo.")
        }
    }
}

// ============================================================================
// PASO 1 Y PASO 2: COLECCIONES, INVENTARIO (MAP) Y FLUJOS DE CONTROL (WHEN)
// ============================================================================

/**
 * PASO 1 (Flujos de Control) & PASO 2 (Colecciones - Map):
 * Evalúa los ingredientes requeridos contra el inventario usando 'when'.
 * Si faltan insumos, detalla los pasos para reabastecerlos.
 */
fun verificarDisponibilidadIngredientes(
    ingredientesRequeridos: Map<String, Int>,
    inventario: Map<String, Int>
): Boolean {
    val faltantes = ingredientesRequeridos.filter { (ingrediente, cantidad) ->
        (inventario[ingrediente] ?: 0) < cantidad
    }

    // Retroalimentación docente: verificar si las existencias disponibles son estrictamente cero
    val todosEnCero = faltantes.isNotEmpty() && faltantes.keys.all { (inventario[it] ?: 0) == 0 }

    // Clasificación del estado mediante when
    val estadoStock = when {
        faltantes.isEmpty() -> "SUFICIENTE"
        faltantes.size == ingredientesRequeridos.size && todosEnCero -> "TOTALMENTE_AGOTADO"
        else -> "PARCIALMENTE_INSUFICIENTE"
    }

    return when (estadoStock) {
        "SUFICIENTE" -> {
            println("✅ [Inventario]: Todos los ingredientes requeridos están disponibles.")
            true
        }
        "PARCIALMENTE_INSUFICIENTE" -> {
            println("❌ [Inventario Insuficiente]: No se puede procesar el pedido. Faltan los siguientes insumos:")
            for ((ingrediente, cantReq) in faltantes) {
                val disponible = inventario[ingrediente] ?: 0
                val falta = cantReq - disponible
                println("   * $ingrediente -> Requiere: $cantReq | En stock: $disponible | Faltan: $falta")
            }
            mostrarPasosReabastecimiento(faltantes.keys)
            false
        }
        "TOTALMENTE_AGOTADO" -> {
            println("❌ [Inventario Crítico]: Ninguno de los ingredientes necesarios se encuentra en stock (existencias en 0).")
            mostrarPasosReabastecimiento(faltantes.keys)
            false
        }
        else -> false
    }
}

/**
 * Imprime los pasos operativos de reabastecimiento solicitados por la pauta.
 */
fun mostrarPasosReabastecimiento(ingredientesFaltantes: Set<String>) {
    println("📌 PROTOCOLO DE REABASTECIMIENTO DE LA PIZZERÍA:")
    println("   1. Generar orden de compra de emergencia para: ${ingredientesFaltantes.joinToString(", ")}.")
    println("   2. Contactar a los distribuidores autorizados de la zona.")
    println("   3. Registrar la reposición con la opción 8 del menú.")
    println("   4. Volver a ingresar el pedido rechazado.")
}

/**
 * PASO 2 (Funciones):
 * Actualiza automáticamente el inventario restando los insumos consumidos.
 * Retroalimentación docente: uso de require(stockActual >= cantidad) antes de restar para evitar descuentos inconsistentes.
 */
fun actualizarInventario(
    ingredientesRequeridos: Map<String, Int>,
    inventario: MutableMap<String, Int>
) {
    for ((ingrediente, cantidad) in ingredientesRequeridos) {
        val stockActual = inventario[ingrediente] ?: 0
        require(stockActual >= cantidad) {
            "Stock insuficiente para '$ingrediente': disponible $stockActual, requerido $cantidad."
        }
        inventario[ingrediente] = stockActual - cantidad
    }
    println("📦 [Inventario Actualizado]: Stock descontado automáticamente en el sistema.")
}

/**
 * PASO 2 (Colecciones - find):
 * Asigna el repartidor disponible más cercano usando la lista y el método .find.
 */
fun asignarRepartidor(repartidores: List<Repartidor>): Repartidor? {
    return repartidores
        .sortedBy { it.distanciaKm }
        .find { it.disponible }
}

// ============================================================================
// PASO 1: REPORTES CON ARREGLOS (ARRAY), CICLO FOR Y FILTER
// ============================================================================

/**
 * PASO 1 (Arreglos):
 * Genera el reporte del día recorriendo un Array<Pedido> con un ciclo 'for',
 * y agrupa pedidos completados y pendientes utilizando '.filter'.
 */
fun generarReporteDelDia(pedidosDelDia: Array<Pedido>) {
    println("\n============================================================")
    println("📊 REPORTE DE GESTIÓN DIARIA DE PEDIDOS (Ciclo FOR)")
    println("============================================================")

    if (pedidosDelDia.isEmpty()) {
        println("⚠️ No hay pedidos registrados en el día de hoy.")
        return
    }

    // Recorrido con ciclo for tal como exige la pauta
    for ((index, p) in pedidosDelDia.withIndex()) {
        val pizzasDesc = p.productos.joinToString { it.nombre }
        val totalFormateado = String.format(Locale.US, "%.2f", p.total)
        println("[#${index + 1}] ID: ${p.id.take(8)} | Cliente: ${p.cliente.nombre} | Pizzas: [$pizzasDesc] | Total: $$totalFormateado | Estado: ${p.estado}")
    }

    println("\n------------------------------------------------------------")
    println("🔍 CLASIFICACIÓN DE PEDIDOS (Uso de .filter)")
    println("------------------------------------------------------------")

    // Agrupación con .filter
    val pedidosCompletados = pedidosDelDia.filter { it.estado == "Entregado" }
    val pedidosPendientes = pedidosDelDia.filter { it.estado != "Entregado" }

    println("🟢 Pedidos Completados (${pedidosCompletados.size}):")
    if (pedidosCompletados.isEmpty()) {
        println("   - Ninguno por el momento.")
    } else {
        for (p in pedidosCompletados) {
            println("   * Pedido #${p.id.take(8)} | Cliente: ${p.cliente.nombre} | Estado: ${p.estado}")
        }
    }

    println("\n🟡 Pedidos Pendientes de Entrega (${pedidosPendientes.size}):")
    if (pedidosPendientes.isEmpty()) {
        println("   - No hay pedidos pendientes.")
    } else {
        for (p in pedidosPendientes) {
            println("   * Pedido #${p.id.take(8)} | Cliente: ${p.cliente.nombre} | Estado: ${p.estado}")
        }
    }
    println("============================================================\n")
}

// ============================================================================
// FUNCIÓN PRINCIPAL Y FLUJO DE EJECUCIÓN
// ============================================================================

fun main() {
    try {
        ejecutarMenu()
    } catch (_: FinDeEntrada) {
        println("\nEntrada finalizada.")
    }
}

private fun ejecutarMenu() {
    println("🍕 =========================================================")
    println("🍕 SISTEMA INTEGRAL DE GESTIÓN - PIZZERÍA KOTLIN (SEMANA 2)")
    println("🍕 =========================================================")

    // PASO 2: Colección Set para registrar categorías del menú (Criterio: listOf - set - Map)
    val categoriasMenu: Set<String> = setOf("Clásicas", "Especiales", "Gourmet", "Vegetarianas")
    println("Categorías habilitadas en sistema: ${categoriasMenu.joinToString(" | ")}")

    // PASO 2: Colección Map para registrar el inventario inicial
    val inventario: MutableMap<String, Int> = mutableMapOf(
        "Masa" to 20,
        "Salsa de Tomate" to 25,
        "Queso Mozzarella" to 20,
        "Pepperoni" to 15,
        "Jamón" to 10,
        "Piña" to 8,
        "Champiñones" to 10,
        "Albahaca" to 10,
        "Bebida Cola" to 15
    )

    // Catálogo de Productos disponibles (incluye pizzas clásicas y productos anotados con @Promocionable)
    val catalogo: List<Producto> = listOf(
        Producto("Pizza Margarita", 8500.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Albahaca"), "Pizza"),
        Producto("Pizza Pepperoni", 9900.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni"), "Pizza"),
        Producto("Pizza Hawaiana", 9500.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Jamón", "Piña"), "Pizza"),
        Producto("Pizza Champiñón", 9200.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Champiñones"), "Pizza"),
        PizzaPromocional("Pizza Promo Suprema", 10500.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni", "Champiñones")),
        ComboFamiliar("Combo Familiar Duoc UC", 16990.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni", "Bebida Cola"))
    )

    // PASO 2: Colección listOf para los repartidores disponibles
    val repartidores: List<Repartidor> = listOf(
        Repartidor("Carlos Silva", "+56911223344", distanciaKm = 1.2, disponible = true),
        Repartidor("María López", "+56955667788", distanciaKm = 2.4, disponible = true),
        Repartidor("Pedro Soto", "+56999887766", distanciaKm = 0.5, disponible = false),
        Repartidor("Andrea Castro", "+56944332211", distanciaKm = 3.1, disponible = true)
    )

    var pedidos = emptyArray<Pedido>()
    while (true) {
        println("""
            
            MENÚ PRINCIPAL
            1. Registrar pedido
            2. Consultar inventario
            3. Ver catálogo
            4. Ver repartidores
            5. Reporte del día
            6. Demostración integral
            7. Avanzar estado de un pedido
            8. Reponer ingrediente
            9. Inspeccionar clases con Reflection (Paso 2)
            10. Probar sobrecarga de operadores (+ y ==) (Paso 3)
            11. Probar desestructuración de objetos (Cliente y Pedido) (Paso 4)
            0. Salir
        """.trimIndent())
        when (leerEntero("Opción: ", 0, 11)) {
            1 -> tomarPedido(catalogo, inventario)?.let { pedidos += it }
            2 -> inventario.forEach { (ingrediente, cantidad) ->
                val estado = if (cantidad > 5) "Óptimo" else if (cantidad > 0) "Bajo" else "Agotado"
                println("$ingrediente | $cantidad unidades | $estado")
            }
            3 -> mostrarCatalogo(catalogo)
            4 -> repartidores.forEach {
                val estado = if (it.disponible) "Disponible" else "Ocupado"
                println("${it.obtenerIdentificacion()} | ${it.distanciaKm} km | $estado")
            }
            5 -> generarReporteDelDia(pedidos)
            6 -> demostrar(catalogo, inventario.toMutableMap(), repartidores.map {
                Repartidor(it.nombre, it.contacto, it.distanciaKm, it.disponible)
            })
            7 -> {
                generarReporteDelDia(pedidos)
                if (pedidos.isNotEmpty()) {
                    val numero = leerEntero("Número de pedido: ", 1, pedidos.size)
                    avanzarPedido(pedidos[numero - 1], repartidores)
                }
            }
            8 -> reponerIngrediente(inventario)
            9 -> {
                println("\n🔬 SELECCIONE LA CLASE A INSPECCIONAR MEDIANTE REFLECTION:")
                println("1. Cliente")
                println("2. Producto")
                println("3. Pedido")
                println("4. ComboFamiliar (@Promocionable)")
                println("5. PizzaPromocional (@Promocionable)")
                println("6. Inspección completa de todas las entidades")
                when (leerEntero("Opción: ", 1, 6)) {
                    1 -> inspeccionarEntidadConReflection(Cliente::class.java)
                    2 -> inspeccionarEntidadConReflection(Producto::class.java)
                    3 -> inspeccionarEntidadConReflection(Pedido::class.java)
                    4 -> inspeccionarEntidadConReflection(ComboFamiliar::class.java)
                    5 -> inspeccionarEntidadConReflection(PizzaPromocional::class.java)
                    6 -> {
                        inspeccionarEntidadConReflection(Cliente::class.java)
                        inspeccionarEntidadConReflection(Producto::class.java)
                        inspeccionarEntidadConReflection(Pedido::class.java)
                        inspeccionarEntidadConReflection(ComboFamiliar::class.java)
                    }
                }
            }
            10 -> {
                val demoCliente = Cliente("Carolina Delgado", "+56 (991) 234-5678", "carolina.delgado@duocuc.cl", "Av. Principal 123")
                demostrarSobrecargaOperadores(catalogo, demoCliente)
            }
            11 -> {
                val demoCliente = Cliente("Carolina Delgado", "+56 (991) 234-5678", "carolina.delgado@duocuc.cl", "Av. Principal 123")
                val demoPedido = Pedido(cliente = demoCliente, productos = listOf(catalogo[0], catalogo[4]), notas = "Demostración Desestructuración")
                demostrarDesestructuracion(demoPedido)
            }
            0 -> return
        }
    }
}

private fun mostrarCatalogo(catalogo: List<Producto>) {
    catalogo.forEachIndexed { indice, producto ->
        println("${indice + 1}. ${producto.detallarProducto()}")
    }
}

private fun tomarPedido(catalogo: List<Producto>, inventario: MutableMap<String, Int>): Pedido? {
    val nombre = leerTextoObligatorio("Nombre del cliente: ")
    val direccion = leerTextoObligatorio("Dirección de entrega: ")
    // PASO 1 (Regex): Formateo de número telefónico uniforme (+XX (XXX) XXX-XXXX)
    val telefono = leerTelefonoValido("Teléfono de contacto: ")
    // PASO 1 (Regex): Validación de correo con reingreso obligatorio si es inválido
    val correo = leerEmailObligatorio("Correo electrónico: ")
    print("Notas especiales (Enter para omitir): ")
    val notas = leer().ifBlank { null }

    mostrarCatalogo(catalogo)
    val productos = mutableListOf<Producto>()
    while (true) {
        val numero = leerEntero("Número de pizza (0 para confirmar pedido): ", 0, catalogo.size)
        if (numero == 0) break
        // PASO 1 (Ranges): Cantidad validada dentro del rango permitido 1..100
        val cantidad = leerCantidadEnRango("Cantidad de pizzas (1 a 100): ")
        repeat(cantidad) { productos.add(catalogo[numero - 1]) }
    }
    if (productos.isEmpty()) {
        println("Pedido cancelado. Agregue al menos una pizza.")
        return null
    }
    return registrarPedido(Cliente(nombre, telefono, correo, direccion), productos, notas, inventario)
}

private fun registrarPedido(
    cliente: Cliente,
    productos: List<Producto>,
    notas: String?,
    inventario: MutableMap<String, Int>
): Pedido? {
    if (!reservarIngredientes(productos.flatMap { it.ingredientes }, inventario)) return null
    val pedido = Pedido(cliente = cliente, productos = productos.toList(), notas = notas)
    cliente.email?.let { println("Confirmación simulada para $it | Pedido #${pedido.id.take(8)}") }

    // PASO 4 (Desestructuración): Demostración en el flujo de pedidos
    val (clienteDestructurado, productosDestructurados, totalDestructurado) = pedido
    val (nombreCli, correoCli, telCli) = clienteDestructurado
    println("📦 [Desestructuración]: Pedido #${pedido.id.take(8)} registrado para $nombreCli (Tel: $telCli, Email: $correoCli)")
    println("   -> Total productos: ${productosDestructurados.size} unidades | Monto: $$${String.format(Locale.US, "%.2f", totalDestructurado)}")

    println(pedido.generarResumen())
    return pedido
}

private fun reservarIngredientes(ingredientes: List<String>, inventario: MutableMap<String, Int>): Boolean {
    val requeridos = ingredientes.groupingBy { it }.eachCount()
    if (!verificarDisponibilidadIngredientes(requeridos, inventario)) return false
    actualizarInventario(requeridos, inventario)
    return true
}

private fun avanzarPedido(pedido: Pedido, repartidores: List<Repartidor>) {
    when (pedido.estado) {
        "En preparación" -> pedido.cambiarEstado("Listo")
        "Listo" -> {
            val repartidor = asignarRepartidor(repartidores)
            if (repartidor == null) {
                println("No hay repartidores disponibles. El pedido sigue listo.")
                return
            }
            pedido.cambiarEstado("En camino")
            repartidor.disponible = false
            pedido.repartidor = repartidor
            println("Repartidor asignado | ${repartidor.nombre}")
        }
        "En camino" -> checkNotNull(pedido.repartidor).entregarPedido(pedido)
        "Entregado" -> println("El pedido ya fue entregado.")
    }
    println(pedido.generarResumen())
}

private fun reponerIngrediente(inventario: MutableMap<String, Int>) {
    val ingredientes = inventario.keys.toList()
    ingredientes.forEachIndexed { indice, nombre -> println("${indice + 1}. $nombre") }
    val ingrediente = ingredientes[leerEntero("Número de ingrediente: ", 1, ingredientes.size) - 1]
    val stock = inventario.getValue(ingrediente)
    if (stock >= 1000) {
        println("El ingrediente ya alcanzó el máximo de unidades.")
        return
    }
    // PASO 1 (Ranges): Cantidad de insumos validada dentro del rango permitido 1..100
    val cantidad = leerCantidadEnRango("Unidades a reponer (1 a 100): ")
    inventario[ingrediente] = stock + cantidad
    println("Reposición registrada | $ingrediente | ${inventario[ingrediente]} unidades")
}

/**
 * PASO 4 (Declaraciones de Desestructuración):
 * Demuestra explícitamente en consola la desestructuración funcional de las entidades
 * Cliente y Pedido, extrayendo sus propiedades en variables individuales según la pauta.
 */
fun demostrarDesestructuracion(pedido: Pedido) {
    println("\n============================================================")
    println("📦 [PASO 4: DECLARACIONES DE DESESTRUCTURACIÓN] DEMOSTRACIÓN")
    println("============================================================")

    // 1. Desestructuración de Cliente: val (nombre, correo, telefono) = cliente
    val (nombreCli, correoCli, telCli) = pedido.cliente
    println("1️⃣  DESESTRUCTURACIÓN DE OBJETO CLIENTE:")
    println("   Declaración ejecutada: val (nombre, correo, telefono) = cliente")
    println("   -> Variable 'nombre':   $nombreCli")
    println("   -> Variable 'correo':   $correoCli")
    println("   -> Variable 'telefono': $telCli")

    // 2. Desestructuración de Pedido: val (cliente, productos, total) = pedido
    val (clienteExtraido, prodsExtraidos, totalExtraido) = pedido
    println("\n2️⃣  DESESTRUCTURACIÓN DE OBJETO PEDIDO:")
    println("   Declaración ejecutada: val (cliente, productos, total) = pedido")
    println("   -> Variable 'cliente':   ${clienteExtraido.nombre} (${clienteExtraido.email})")
    println("   -> Variable 'productos': ${prodsExtraidos.size} unidades [${prodsExtraidos.joinToString { it.nombre }}]")
    println("   -> Variable 'total':     $$${String.format(Locale.US, "%.2f", totalExtraido)}")
    println("============================================================\n")
}

/**
 * PASO 3 (Sobrecarga de Operadores):
 * Demuestra explícitamente en consola la sobrecarga de los 2 operadores exigidos:
 * 1. Operador '+' en Pedido: combina dos pedidos y recalcula montos, incluyendo descuentos.
 * 2. Operador '==' en Producto: compara si dos productos representan la misma entidad por nombre y categoría.
 */
fun demostrarSobrecargaOperadores(catalogo: List<Producto>, cliente: Cliente) {
    println("\n============================================================")
    println("➕ [PASO 3: SOBRECARGA DE OPERADORES] DEMOSTRACIÓN")
    println("============================================================")

    println("1️⃣  SOBRECARGA DEL OPERADOR '==' EN PRODUCTO (por nombre y categoría):")
    val prod1 = Producto("Pizza Pepperoni", 9900.0, listOf("Masa", "Salsa", "Pepperoni"), "Pizza")
    val prod2 = Producto("pizza pepperoni", 11500.0, listOf("Masa", "Tomate"), "Pizza") // Mismo nombre y categoría, precio distinto
    val prod3 = Producto("Pizza Pepperoni", 9900.0, listOf("Masa", "Salsa", "Pepperoni"), "Combo") // Distinta categoría

    println("   - Producto A: [${prod1.categoria}] '${prod1.nombre}' ($${prod1.precio})")
    println("   - Producto B: [${prod2.categoria}] '${prod2.nombre}' ($${prod2.precio})")
    println("   - Producto C: [${prod3.categoria}] '${prod3.nombre}' ($${prod3.precio})")
    println("   -> Evaluación (prod1 == prod2) [mismo nombre y categoría]: ${prod1 == prod2} (Resultado: true)")
    println("   -> Evaluación (prod1 == prod3) [distinta categoría]:       ${prod1 == prod3} (Resultado: false)")

    println("\n2️⃣  SOBRECARGA DEL OPERADOR '+' EN PEDIDO (fusión de dos pedidos):")
    val pedido1 = Pedido(cliente = cliente, productos = listOf(catalogo[0], catalogo[1]), notas = "Mesa 1")
    val pedido2 = Pedido(cliente = cliente, productos = listOf(catalogo[4], catalogo[5]), notas = "Extra servilletas")

    println("   - Pedido 1 (#${pedido1.id.take(8)}): ${pedido1.productos.size} productos | Subtotal: $$${String.format(Locale.US, "%.2f", pedido1.subtotal)} | Total: $$${String.format(Locale.US, "%.2f", pedido1.total)}")
    println("   - Pedido 2 (#${pedido2.id.take(8)}): ${pedido2.productos.size} productos | Subtotal: $$${String.format(Locale.US, "%.2f", pedido2.subtotal)} | Total: $$${String.format(Locale.US, "%.2f", pedido2.total)}")

    // USO DEL OPERADOR '+'
    val pedidoCombinado = pedido1 + pedido2

    println("\n   ✨ RESULTADO TRAS: val pedidoCombinado = pedido1 + pedido2")
    println("   - Nuevo ID Pedido:        #${pedidoCombinado.id.take(8)}")
    println("   - Total ítems combinados: ${pedidoCombinado.productos.size} unidades")
    println("   - Notas combinadas:       '${pedidoCombinado.notas}'")
    println("   - Subtotal combinado:     $$${String.format(Locale.US, "%.2f", pedidoCombinado.subtotal)}")
    println("   - Descuentos aplicados:  -$${String.format(Locale.US, "%.2f", pedidoCombinado.descuento)}")
    println("   - TOTAL FINAL A PAGAR:    $$${String.format(Locale.US, "%.2f", pedidoCombinado.total)}")
    println("============================================================\n")
}

private fun demostrar(
    catalogo: List<Producto>,
    inventario: MutableMap<String, Int>,
    repartidores: List<Repartidor>
) {
    println("\nDEMOSTRACIÓN CON DATOS DE EJEMPLO")
    val cliente = Cliente("Valentina Flores", "+56 (991) 234-5678", "valentina@example.com", "Av. Providencia 1234")
    val pizzas = listOf(catalogo[0], catalogo[1], catalogo[4], catalogo[5])
    var pedidos = emptyArray<Pedido>()
    registrarPedido(cliente, pizzas, null, inventario)?.let { pedido ->
        pedidos += pedido
        repeat(3) { avanzarPedido(pedido, repartidores) }
    }

    val otroCliente = Cliente("Gonzalo Pérez", formatearTelefono("56912345678"), "gonzalo@dominio.com", "Calle Los Aromos 45")
    registrarPedido(otroCliente, listOf(catalogo[0]), "Tocar el timbre", inventario)?.let { pedidos += it }
    generarReporteDelDia(pedidos)

    // Demostración de sobrecarga de operadores (+ y ==)
    demostrarSobrecargaOperadores(catalogo, cliente)

    // Demostración de desestructuración (Cliente y Pedido)
    pedidos.firstOrNull()?.let { demostrarDesestructuracion(it) }
}
