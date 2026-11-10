import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * ============================================================================
 * SISTEMA INTEGRAL DE GESTIÓN DE PIZZERÍA
 * Aplicación de consola en Kotlin con utilidades avanzadas:
 * - Expresiones Regulares (Regex) y Rangos (Ranges)
 * - Anotaciones personalizadas y Reflection dinámico
 * - Sobrecarga de Operadores (Operator Overloading)
 * - Declaraciones de Desestructuración
 * - Evaluación de Igualdad (equals y hashCode) y Control de Duplicados
 * ============================================================================
 */

// ============================================================================
// SECCIÓN 1: MODELO DEL DOMINIO Y JERARQUÍA DE CLASES (POO)
// ============================================================================

/**
 * Clase base que modela a un usuario del sistema.
 */
open class Usuario(
    val nombre: String,
    val contacto: String
) {
    open fun obtenerIdentificacion(): String = "$nombre ($contacto)"
}

/**
 * Modela a un cliente del restaurante.
 * Incluye operadores de desestructuración y evaluación de igualdad estructural.
 */
class Cliente(
    nombre: String,
    contacto: String,
    val email: String?,
    val direccion: String
) : Usuario(nombre.trim(), contacto.trim()) {

    fun obtenerFichaCliente(): String =
        "Cliente: $nombre | Teléfono: $contacto | Email: ${email ?: "Sin correo"} | Dirección: $direccion"

    /**
     * Permite la desestructuración posicional de la instancia:
     * val (nombre, correo, telefono) = cliente
     */
    operator fun component1(): String = nombre
    operator fun component2(): String = email ?: "Sin correo"
    operator fun component3(): String = contacto
    operator fun component4(): String = direccion

    /**
     * Dos clientes se consideran idénticos si coinciden en nombre y correo electrónico
     * (independientemente de mayúsculas/minúsculas).
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

    override fun toString(): String = "$nombre <${email ?: "sin correo"}> $contacto"
}

/**
 * Modela a un repartidor encargado del despacho de pedidos.
 */
class Repartidor(
    nombre: String,
    contacto: String,
    val distanciaKm: Double,
    var disponible: Boolean = true
) : Usuario(nombre, contacto) {

    fun entregarPedido(pedido: Pedido) {
        require(pedido.repartidor === this) { "El pedido está asignado a otro repartidor." }
        pedido.cambiarEstado("Entregado")
        this.disponible = true
        println("🚴 [Repartidor $nombre]: Ha entregado con éxito el pedido #${pedido.id.take(8)} en ${pedido.cliente.direccion}.")
    }
}

// ============================================================================
// SECCIÓN 2: ANOTACIONES PERSONALIZADAS Y METAPROGRAMACIÓN
// ============================================================================

/**
 * Anotación personalizada para marcar clases de productos o combos
 * que califican para promociones especiales en el sistema.
 * Configurada para estar disponible en tiempo de ejecución (RUNTIME).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Promocionable(
    val descuento: Int = 10,
    val etiqueta: String = "Oferta Especial"
)

/**
 * Modela un producto del catálogo con nombre, precio, ingredientes y categoría.
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

    open fun detallarProducto(): String {
        val precioFormateado = String.format(Locale.US, "%.2f", precio)
        val promo = this.promocion()
        val etiquetaPromo = if (promo != null) " ⭐ [${promo.etiqueta} -${promo.descuento}%]" else ""
        return "[$categoria] $nombre - $$precioFormateado$etiquetaPromo | Ingredientes: ${ingredientes.joinToString(", ")}"
    }

    /**
     * Sobrecarga del operador '==' mediante equals():
     * Dos productos representan el mismo ítem si coinciden en su nombre y categoría.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Producto) return false
        return this.nombre.equals(other.nombre, ignoreCase = true) &&
               this.categoria.equals(other.categoria, ignoreCase = true)
    }

    override fun hashCode(): Int {
        var result = nombre.lowercase().hashCode()
        result = 31 * result + categoria.lowercase().hashCode()
        return result
    }

    override fun toString(): String = "[$categoria] $nombre ($${String.format(Locale.US, "%.2f", precio)})"
}

/**
 * Funciones de extensión para consultar la anotación @Promocionable vía Reflection.
 */
fun Producto.promocion(): Promocionable? =
    this::class.java.getAnnotation(Promocionable::class.java)

fun Producto.esPromocionable(): Boolean =
    this.promocion() != null

/**
 * Clase ComboFamiliar con promoción del 15%.
 */
@Promocionable(descuento = 15, etiqueta = "Combo Familiar Gourmet")
open class ComboFamiliar(
    nombre: String = "Combo Familiar Gourmet",
    precio: Double = 16990.0,
    ingredientes: List<String> = listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni", "Bebida Cola")
) : Producto(nombre, precio, ingredientes, "Combo")

/**
 * Clase PizzaPromocional con promoción del 10%.
 */
@Promocionable(descuento = 10, etiqueta = "Promo Suprema Temporada")
open class PizzaPromocional(
    nombre: String = "Pizza Promo Suprema",
    precio: Double = 10500.0,
    ingredientes: List<String> = listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni", "Champiñones")
) : Producto(nombre, precio, ingredientes, "Pizza")

// ============================================================================
// SECCIÓN 3: REGLAS DE PROMOCIONES TEMPORALES Y VALIDACIONES (REGEX & RANGES)
// ============================================================================

val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Singleton que gestiona el período de promociones por rango de fechas.
 */
object Promociones {
    val FECHA_INICIO_PROMO: LocalDate = LocalDate.now().minusDays(3)
    val FECHA_FIN_PROMO: LocalDate = LocalDate.now().plusDays(10)
    val PERIODO_PROMOCION: ClosedRange<LocalDate> = FECHA_INICIO_PROMO..FECHA_FIN_PROMO
    const val DESCUENTO_TEMPORADA_PORCENTAJE = 15

    fun estaEnTemporada(fecha: LocalDate): Boolean = fecha in PERIODO_PROMOCION
}

/**
 * Singleton que centraliza las validaciones de datos con Regex y Rangos.
 */
object Validaciones {
    private val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val REGEX_NO_DIGITO = Regex("\\D")
    private val REGEX_TELEFONO = Regex("(\\d{2})(\\d{3})(\\d{3})(\\d{4})")
    val RANGO_CANTIDAD: IntRange = 1..100
    const val CODIGO_PAIS_DEFECTO = "56"

    fun esCorreoValido(correo: String): Boolean =
        REGEX_CORREO.matches(correo.trim())

    /**
     * Normaliza y formatea cualquier número de teléfono al formato +XX (XXX) XXX-XXXX.
     */
    fun formatearTelefono(entrada: String): String? {
        val digitos = entrada.replace(REGEX_NO_DIGITO, "")
        val completo = when (digitos.length) {
            9 -> CODIGO_PAIS_DEFECTO + "9" + digitos.takeLast(8)
            10 -> CODIGO_PAIS_DEFECTO + digitos
            11 -> digitos.padStart(12, '0')
            12 -> digitos
            else -> if (digitos.length in 8..15) digitos.padStart(12, '0') else return null
        }
        val coincidencia = REGEX_TELEFONO.matchEntire(completo) ?: return null
        val (pais, area, bloque1, bloque2) = coincidencia.destructured
        return "+$pais ($area) $bloque1-$bloque2"
    }

    fun esCantidadValida(cantidad: Int): Boolean =
        cantidad in RANGO_CANTIDAD
}

// ============================================================================
// SECCIÓN 4: CLASE PEDIDO, OPERADORES Y DESESTRUCTURACIÓN
// ============================================================================

/**
 * Modela un pedido completo de la pizzería.
 * Soporta sobrecarga de operador '+' y declaraciones de desestructuración.
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
        require(productos.isNotEmpty()) { "El pedido debe contener al menos un producto." }
        calcularTotal()
    }

    /**
     * Calcula los montos aplicando:
     * - Descuento por rango de fechas promocionales (15%)
     * - Descuento dinámico por productos marcados con @Promocionable
     * - Descuento por volumen para más de 3 unidades (10%)
     */
    fun calcularTotal() {
        subtotal = productos.sumOf { it.precio }

        // Evaluación de fecha en rango promocional
        val enRangoPromo = Promociones.estaEnTemporada(fecha)
        descuentoPromoFechas = if (enRangoPromo) subtotal * (Promociones.DESCUENTO_TEMPORADA_PORCENTAJE / 100.0) else 0.0

        // Evaluación de productos con anotación @Promocionable
        val prodsPromo = productos.filter { it.esPromocionable() }
        descuentoAnotacionPromo = prodsPromo.sumOf { prod ->
            val promo = prod.promocion()
            val pct = (promo?.descuento ?: 10).toDouble() / 100.0
            prod.precio * pct
        }

        // Descuento por volumen (> 3 unidades)
        descuentoVolumen = if (productos.size > 3) subtotal * 0.10 else 0.0

        descuento = descuentoPromoFechas + descuentoVolumen + descuentoAnotacionPromo
        val baseImponible = (subtotal - descuento).coerceAtLeast(0.0)

        // Impuesto (IVA estándar 19%)
        impuestos = baseImponible * 0.19
        total = baseImponible + impuestos
    }

    /**
     * Sobrecarga del operador '+' para combinar dos pedidos en uno solo.
     * Fusiona las listas de productos, combina notas y recalcula montos automáticamente.
     */
    operator fun plus(otro: Pedido): Pedido {
        val productosCombinados = this.productos + otro.productos
        val notasCombinadas = when {
            this.notas != null && otro.notas != null -> "${this.notas} | ${otro.notas}"
            this.notas != null -> this.notas
            otro.notas != null -> otro.notas
            else -> null
        }
        return Pedido(
            cliente = this.cliente,
            productos = productosCombinados,
            notas = notasCombinadas,
            fecha = if (this.fecha >= otro.fecha) this.fecha else otro.fecha
        )
    }

    /**
     * Operadores de desestructuración para Pedido:
     * val (cliente, productos, total) = pedido
     */
    operator fun component1(): Cliente = cliente
    operator fun component2(): List<Producto> = productos
    operator fun component3(): Double = total
    operator fun component4(): String = id
    operator fun component5(): String = estado

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

        val notasFormateadas = notas?.takeIf { it.isNotBlank() } ?: "Sin notas especiales"

        val repartidorInfo = repartidor?.let { "${it.nombre} (${it.distanciaKm} km de distancia)" }
            ?: "Pendiente de asignación"

        val descPromoFechasTexto = if (descuentoPromoFechas > 0.0) {
            "\n   - Descuento Rango Fechas (15%): -$${String.format(Locale.US, "%.2f", descuentoPromoFechas)} [Promoción activa: ${Promociones.FECHA_INICIO_PROMO.format(FORMATO_FECHA)} al ${Promociones.FECHA_FIN_PROMO.format(FORMATO_FECHA)}]"
        } else ""

        val descAnotacionTexto = if (descuentoAnotacionPromo > 0.0) {
            "\n   - Descuento @Promocionable:      -$${String.format(Locale.US, "%.2f", descuentoAnotacionPromo)} (Identificado dinámicamente con Reflection)"
        } else ""

        val descVolumenTexto = if (descuentoVolumen > 0.0) {
            "\n   - Descuento por Volumen (10%):  -$${String.format(Locale.US, "%.2f", descuentoVolumen)} (>3 unidades)"
        } else ""

        val prodsPromo = productos.filter { it.esPromocionable() }.distinctBy { it.nombre }
        val seccionPromos = if (prodsPromo.isNotEmpty()) {
            "\n------------------------------------------------------------\n" +
            "⭐ PRODUCTOS PROMOCIONABLES DETECTADOS (@Promocionable):\n" +
            prodsPromo.joinToString("\n") { prod ->
                val meta = prod.promocion()
                "   * ${prod.nombre} -> Descuento especial: ${meta?.descuento ?: 10}% [${meta?.etiqueta ?: "Oferta"}]"
            }
        } else ""

        return """
        ============================================================
        🧾 RESUMEN DEL PEDIDO #${id.take(8)} | Fecha: ${fecha.format(FORMATO_FECHA)}
        ============================================================
        👤 DATOS DEL CLIENTE:
           - Nombre:    ${cliente.nombre}
           - Teléfono:  ${cliente.contacto}
           - Correo:    ${cliente.email ?: "Sin correo"}
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
// SECCIÓN 5: INSPECTOR DE REFLECTION
// ============================================================================

/**
 * Inspecciona dinámicamente cualquier clase del dominio mediante Reflection.
 */
fun inspeccionarEntidadConReflection(clase: Class<*>) {
    println("\n============================================================")
    println("🔬 [REFLECTION] INSPECCIÓN DINÁMICA DE LA CLASE: ${clase.simpleName}")
    println("============================================================")

    val anotaciones = clase.annotations
    println("🏷️  Anotaciones presentes (${anotaciones.size}):")
    if (anotaciones.isEmpty()) {
        println("   (Sin anotaciones a nivel de clase)")
    } else {
        anotaciones.forEach { anot ->
            println("   * @${anot.annotationClass.simpleName} -> $anot")
        }
    }

    val campos = clase.declaredFields
    println("\n📋 Atributos / Propiedades declaradas (${campos.size}):")
    campos.forEach { campo ->
        campo.isAccessible = true
        println("   - ${campo.name}: ${campo.type.simpleName}")
    }

    val metodos = clase.declaredMethods
    println("\n⚙️  Métodos declarados (${metodos.size}):")
    metodos.forEach { metodo ->
        val params = metodo.parameterTypes.joinToString(", ") { it.simpleName }
        println("   - ${metodo.name}($params): ${metodo.returnType.simpleName}")
    }
    println("============================================================")
}

// ============================================================================
// SECCIÓN 6: ENTRADA Y VALIDACIÓN INTERACTIVA DE DATOS
// ============================================================================

private class FinDeEntrada : RuntimeException()

private fun leer(): String = readlnOrNull()?.trim() ?: throw FinDeEntrada()

fun leerEntero(mensaje: String, minimo: Int = 0, maximo: Int = Int.MAX_VALUE): Int {
    while (true) {
        print(mensaje)
        val linea = leer()
        try {
            if (linea.isEmpty()) throw IllegalArgumentException("La entrada no puede estar vacía.")
            val numero = linea.toInt()
            if (numero !in minimo..maximo) throw IllegalArgumentException("Ingrese un número entre $minimo y $maximo.")
            return numero
        } catch (e: NumberFormatException) {
            println("❌ Error: Se esperaba un número entero válido. Intente nuevamente.")
        } catch (e: IllegalArgumentException) {
            println("❌ Error: ${e.message} Intente nuevamente.")
        }
    }
}

fun leerTextoObligatorio(mensaje: String): String {
    while (true) {
        print(mensaje)
        val linea = leer()
        try {
            if (linea.isEmpty()) throw IllegalArgumentException("El campo solicitado es obligatorio.")
            return linea
        } catch (e: IllegalArgumentException) {
            println("❌ Error: ${e.message} Intente de nuevo.")
        }
    }
}

fun leerEmailObligatorio(mensaje: String = "Correo electrónico (ej. nombre@dominio.com): "): String {
    while (true) {
        print(mensaje)
        val entrada = leer().trim()
        if (Validaciones.esCorreoValido(entrada)) {
            return entrada
        } else {
            println("❌ Error: El correo '$entrada' no cumple con el formato estándar (nombre@dominio.com). Reingréselo.")
        }
    }
}

fun leerTelefonoValido(mensaje: String = "Teléfono de contacto: "): String {
    while (true) {
        print(mensaje)
        val entrada = leer().trim()
        val formateado = Validaciones.formatearTelefono(entrada)
        if (formateado != null) {
            println("📞 Teléfono normalizado con Regex: $formateado")
            return formateado
        } else {
            println("❌ Error: Teléfono inválido ('$entrada'). Ingrese entre 8 y 15 dígitos. Reintente.")
        }
    }
}

fun leerCantidadEnRango(
    mensaje: String = "Cantidad (1 a 100): ",
    rango: IntRange = Validaciones.RANGO_CANTIDAD
): Int {
    while (true) {
        print(mensaje)
        val linea = leer().trim()
        try {
            val valor = linea.toInt()
            if (Validaciones.esCantidadValida(valor)) {
                return valor
            } else {
                println("❌ Error: La cantidad ingresada ($valor) está fuera del rango permitido (${rango.first}..${rango.last}). Ingrese un valor válido.")
            }
        } catch (e: NumberFormatException) {
            println("❌ Error: Se esperaba un número entero válido. Intente nuevamente.")
        }
    }
}

fun formatearTelefono(telefonoInput: String): String =
    Validaciones.formatearTelefono(telefonoInput) ?: "+56 (900) 000-0000"

fun validarTelefono(telefonoInput: String): String = formatearTelefono(telefonoInput)

fun validarEmail(emailInput: String): String? =
    if (Validaciones.esCorreoValido(emailInput)) emailInput.trim() else null

// ============================================================================
// SECCIÓN 7: GESTIÓN DE INVENTARIO Y REGLAS OPERATIVAS
// ============================================================================

fun verificarDisponibilidadIngredientes(
    ingredientesRequeridos: Map<String, Int>,
    inventario: Map<String, Int>
): Boolean {
    val faltantes = ingredientesRequeridos.filter { (ingrediente, cantidad) ->
        (inventario[ingrediente] ?: 0) < cantidad
    }

    val todosEnCero = faltantes.isNotEmpty() && faltantes.keys.all { (inventario[it] ?: 0) == 0 }

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

fun mostrarPasosReabastecimiento(ingredientesFaltantes: Set<String>) {
    println("📌 PROTOCOLO DE REABASTECIMIENTO DE LA PIZZERÍA:")
    println("   1. Generar orden de compra de emergencia para: ${ingredientesFaltantes.joinToString(", ")}.")
    println("   2. Contactar a los distribuidores autorizados de la zona.")
    println("   3. Registrar la reposición con la opción 8 del menú.")
    println("   4. Volver a ingresar el pedido rechazado.")
}

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

fun asignarRepartidor(repartidores: List<Repartidor>): Repartidor? =
    repartidores.sortedBy { it.distanciaKm }.find { it.disponible }

fun generarReporteDelDia(pedidosDelDia: Array<Pedido>) {
    println("\n============================================================")
    println("📊 REPORTE DE GESTIÓN DIARIA DE PEDIDOS")
    println("============================================================")

    if (pedidosDelDia.isEmpty()) {
        println("⚠️ No hay pedidos registrados en el día de hoy.")
        return
    }

    for ((index, p) in pedidosDelDia.withIndex()) {
        val pizzasDesc = p.productos.joinToString { it.nombre }
        val totalFormateado = String.format(Locale.US, "%.2f", p.total)
        println("[#${index + 1}] ID: ${p.id.take(8)} | Cliente: ${p.cliente.nombre} | Productos: [$pizzasDesc] | Total: $$totalFormateado | Estado: ${p.estado}")
    }

    println("\n------------------------------------------------------------")
    println("🔍 CLASIFICACIÓN DE PEDIDOS (Uso de filter)")
    println("------------------------------------------------------------")

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
// SECCIÓN 8: DEMOSTRACIONES TÉCNICAS MODULARES
// ============================================================================

fun demostrarDesestructuracion(pedido: Pedido) {
    println("\n============================================================")
    println("📦 DEMOSTRACIÓN DE DESESTRUCTURACIÓN DE OBJETOS")
    println("============================================================")

    // Desestructuración de Cliente: val (nombre, correo, telefono) = cliente
    val (nombreCli, correoCli, telCli) = pedido.cliente
    println("1️⃣  DESESTRUCTURACIÓN DE OBJETO CLIENTE:")
    println("   val (nombre, correo, telefono) = cliente")
    println("   -> Variable 'nombre':   $nombreCli")
    println("   -> Variable 'correo':   $correoCli")
    println("   -> Variable 'telefono': $telCli")

    // Desestructuración de Pedido: val (cliente, productos, total) = pedido
    val (clienteExtraido, prodsExtraidos, totalExtraido) = pedido
    println("\n2️⃣  DESESTRUCTURACIÓN DE OBJETO PEDIDO:")
    println("   val (cliente, productos, total) = pedido")
    println("   -> Variable 'cliente':   ${clienteExtraido.nombre} (${clienteExtraido.email})")
    println("   -> Variable 'productos': ${prodsExtraidos.size} unidades [${prodsExtraidos.joinToString { it.nombre }}]")
    println("   -> Variable 'total':     $$${String.format(Locale.US, "%.2f", totalExtraido)}")
    println("============================================================\n")
}

fun demostrarSobrecargaOperadores(catalogo: List<Producto>, cliente: Cliente) {
    println("\n============================================================")
    println("➕ DEMOSTRACIÓN DE SOBRECARGA DE OPERADORES (+ y ==)")
    println("============================================================")

    println("1️⃣  SOBRECARGA DEL OPERADOR '==' EN PRODUCTO (por nombre y categoría):")
    val prod1 = Producto("Pizza Pepperoni", 9900.0, listOf("Masa", "Salsa", "Pepperoni"), "Pizza")
    val prod2 = Producto("pizza pepperoni", 11500.0, listOf("Masa", "Tomate"), "Pizza")
    val prod3 = Producto("Pizza Pepperoni", 9900.0, listOf("Masa", "Salsa", "Pepperoni"), "Combo")

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

fun demostrarPrevencionDuplicados(catalogo: List<Producto>) {
    println("\n============================================================")
    println("🛡️ DEMOSTRACIÓN DE IGUALDAD (equals/hashCode) Y DUPLICADOS")
    println("============================================================")

    println("1️⃣  PREVENCIÓN DE DUPLICADOS EN CLIENTES (Nombre + Correo):")
    val clienteOriginal = Cliente("Carolina Delgado", "+56 (991) 234-5678", "carolina.delgado@mail.com", "Av. Principal 123")
    val clienteDuplicado = Cliente("carolina delgado", "+56 (988) 776-5544", "CAROLINA.DELGADO@MAIL.COM", "Otra Dirección 456")

    println("   - Cliente 1: '${clienteOriginal.nombre}' | Email: '${clienteOriginal.email}'")
    println("   - Cliente 2: '${clienteDuplicado.nombre}' | Email: '${clienteDuplicado.email}'")
    println("   -> ¿Son iguales según equals()? (clienteOriginal == clienteDuplicado): ${clienteOriginal == clienteDuplicado}")
    println("   -> HashCode Cliente 1: ${clienteOriginal.hashCode()}")
    println("   -> HashCode Cliente 2: ${clienteDuplicado.hashCode()} (Mismo hash code)")

    val registroClientes: MutableSet<Cliente> = mutableSetOf()
    val agregado1 = registroClientes.add(clienteOriginal)
    val agregado2 = registroClientes.add(clienteDuplicado)

    println("   -> Intento 1: Agregar Cliente 1 a Set -> ¿Aceptado?: $agregado1")
    println("   -> Intento 2: Agregar Cliente 2 a Set -> ¿Aceptado?: $agregado2 (Rechazado por duplicidad)")
    println("   -> Clientes únicos en el registro: ${registroClientes.size} cliente(s)")

    println("\n2️⃣  PREVENCIÓN DE DUPLICADOS EN PRODUCTOS (Nombre + Categoría):")
    val prodOriginal = Producto("Pizza Pepperoni", 9900.0, listOf("Masa", "Salsa", "Pepperoni"), "Pizza")
    val prodRepetido = Producto("pizza pepperoni", 11500.0, listOf("Masa", "Tomate"), "Pizza")

    println("   - Producto 1: [${prodOriginal.categoria}] '${prodOriginal.nombre}' ($${prodOriginal.precio})")
    println("   - Producto 2: [${prodRepetido.categoria}] '${prodRepetido.nombre}' ($${prodRepetido.precio})")
    println("   -> ¿Son iguales según equals()? (prodOriginal == prodRepetido): ${prodOriginal == prodRepetido}")
    println("   -> HashCode Producto 1: ${prodOriginal.hashCode()}")
    println("   -> HashCode Producto 2: ${prodRepetido.hashCode()}")

    val catalogoSet: MutableSet<Producto> = catalogo.toMutableSet()
    val tamanoInicial = catalogoSet.size
    val prodAgregado = catalogoSet.add(prodRepetido)

    println("   -> Intentando registrar producto repetido en catálogo Set (tamaño inicial: $tamanoInicial):")
    println("   -> ¿Fue aceptado?: $prodAgregado (Rechazado: ya existe un producto con mismo nombre y categoría)")
    println("   -> Tamaño final del catálogo: ${catalogoSet.size} productos")
    println("============================================================\n")
}

// ============================================================================
// SECCIÓN 9: FLUJO PRINCIPAL Y MENÚ DE EJECUCIÓN
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
    println("🍕 SISTEMA INTEGRAL DE GESTIÓN DE PIZZERÍA KOTLIN")
    println("🍕 =========================================================")

    val categoriasMenu: Set<String> = setOf("Clásicas", "Especiales", "Gourmet", "Vegetarianas", "Combos")
    println("Categorías habilitadas en sistema: ${categoriasMenu.joinToString(" | ")}")

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

    val catalogo: List<Producto> = listOf(
        Producto("Pizza Margarita", 8500.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Albahaca"), "Pizza"),
        Producto("Pizza Pepperoni", 9900.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni"), "Pizza"),
        Producto("Pizza Hawaiana", 9500.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Jamón", "Piña"), "Pizza"),
        Producto("Pizza Champiñón", 9200.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Champiñones"), "Pizza"),
        PizzaPromocional("Pizza Promo Suprema", 10500.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni", "Champiñones")),
        ComboFamiliar("Combo Familiar Gourmet", 16990.0, listOf("Masa", "Salsa de Tomate", "Queso Mozzarella", "Pepperoni", "Bebida Cola"))
    )

    val repartidores: List<Repartidor> = listOf(
        Repartidor("Carlos Silva", "+56 (911) 223-3440", distanciaKm = 1.2, disponible = true),
        Repartidor("María López", "+56 (955) 667-7880", distanciaKm = 2.4, disponible = true),
        Repartidor("Pedro Soto", "+56 (999) 887-7660", distanciaKm = 0.5, disponible = false),
        Repartidor("Andrea Castro", "+56 (944) 332-2110", distanciaKm = 3.1, disponible = true)
    )

    val clientesRegistrados: MutableSet<Cliente> = mutableSetOf()
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
            9. Inspeccionar clases con Reflection
            10. Probar sobrecarga de operadores (+ y ==)
            11. Probar desestructuración de objetos (Cliente y Pedido)
            12. Probar igualdad y prevención de duplicados
            0. Salir
        """.trimIndent())
        when (leerEntero("Opción: ", 0, 12)) {
            1 -> tomarPedido(catalogo, inventario, clientesRegistrados)?.let { pedidos += it }
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
                val demoCliente = Cliente("Carolina Delgado", "+56 (991) 234-5678", "carolina.delgado@mail.com", "Av. Principal 123")
                demostrarSobrecargaOperadores(catalogo, demoCliente)
            }
            11 -> {
                val demoCliente = Cliente("Carolina Delgado", "+56 (991) 234-5678", "carolina.delgado@mail.com", "Av. Principal 123")
                val demoPedido = Pedido(cliente = demoCliente, productos = listOf(catalogo[0], catalogo[4]), notas = "Demostración Desestructuración")
                demostrarDesestructuracion(demoPedido)
            }
            12 -> demostrarPrevencionDuplicados(catalogo)
            0 -> return
        }
    }
}

private fun mostrarCatalogo(catalogo: List<Producto>) {
    catalogo.forEachIndexed { indice, producto ->
        println("${indice + 1}. ${producto.detallarProducto()}")
    }
}

private fun tomarPedido(
    catalogo: List<Producto>,
    inventario: MutableMap<String, Int>,
    clientesRegistrados: MutableSet<Cliente> = mutableSetOf()
): Pedido? {
    val nombre = leerTextoObligatorio("Nombre del cliente: ")
    val direccion = leerTextoObligatorio("Dirección de entrega: ")
    val telefono = leerTelefonoValido("Teléfono de contacto: ")
    val correo = leerEmailObligatorio("Correo electrónico: ")
    print("Notas especiales (Enter para omitir): ")
    val notas = leer().ifBlank { null }

    val cliente = Cliente(nombre, telefono, correo, direccion)
    if (clientesRegistrados.add(cliente)) {
        println("👤 [Registro Clientes]: Nuevo cliente registrado en el padrón de la pizzería.")
    } else {
        println("👤 [Registro Clientes]: Cliente habitual detectado vía equals()/hashCode(). No se duplica en el sistema.")
    }

    mostrarCatalogo(catalogo)
    val productos = mutableListOf<Producto>()
    while (true) {
        val numero = leerEntero("Número de pizza (0 para confirmar pedido): ", 0, catalogo.size)
        if (numero == 0) break
        val cantidad = leerCantidadEnRango("Cantidad de pizzas (1 a 100): ")
        repeat(cantidad) { productos.add(catalogo[numero - 1]) }
    }
    if (productos.isEmpty()) {
        println("Pedido cancelado. Agregue al menos una pizza.")
        return null
    }
    return registrarPedido(cliente, productos, notas, inventario)
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
    val cantidad = leerCantidadEnRango("Unidades a reponer (1 a 100): ")
    inventario[ingrediente] = stock + cantidad
    println("Reposición registrada | $ingrediente | ${inventario[ingrediente]} unidades")
}

private fun demostrar(
    catalogo: List<Producto>,
    inventario: MutableMap<String, Int>,
    repartidores: List<Repartidor>
) {
    println("\n============================================================")
    println("🚀 INICIO DE DEMOSTRACIÓN INTEGRAL")
    println("============================================================")

    val cliente = Cliente("Valentina Flores", "+56 (991) 234-5678", "valentina@mail.com", "Av. Providencia 1234")
    val pizzas = listOf(catalogo[0], catalogo[1], catalogo[4], catalogo[5])
    var pedidos = emptyArray<Pedido>()

    println("\n--- REGISTRO DE PEDIDO CON PRODUCTOS PROMOCIONABLES ---")
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

    // Demostración de prevención de duplicados (equals y hashCode)
    demostrarPrevencionDuplicados(catalogo)

    // Demostración de Reflection
    println("\n--- INSPECCIÓN DINÁMICA CON REFLECTION ---")
    inspeccionarEntidadConReflection(ComboFamiliar::class.java)
    inspeccionarEntidadConReflection(Cliente::class.java)
}
