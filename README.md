# PMY2201_Exp1_S3_Carolina_Delgado

**Proyecto:** Sistema de Gestión de Pizzería en Kotlin  
**Autora:** Carolina Delgado  

---

## 📌 Descripción del Proyecto

Sistema integral para la gestión comercial y operativa de una pizzería, implementado con características avanzadas de Kotlin:

- **Expresiones Regulares (Regex) y Rangos (Ranges):**
  - Validación de correo electrónico (`nombre@dominio.com`) con solicitud de reingreso ante formato incorrecto.
  - Normalización y formateo uniforme de teléfonos internacionales (`+XX (XXX) XXX-XXXX`) mediante Regex y grupos de captura desestructurados.
  - Gestión de promociones por temporada mediante rangos de fechas cerrados (`ClosedRange<LocalDate>`), aplicando descuentos automáticos.
  - Validación de rangos numéricos permitidos (`1..100`) para pedidos e insumos.
- **Metaprogramación mediante Anotaciones y Reflection:**
  - Anotación personalizada `@Promocionable` con retención en tiempo de ejecución (`RUNTIME`) para identificar productos y combos con descuentos especiales.
  - Identificación dinámica de promociones en tiempo de ejecución mediante funciones de extensión y Reflection.
  - Módulo inspector que examina dinámicamente propiedades, métodos y anotaciones de cualquier entidad.
- **Sobrecarga de Operadores (Operator Overloading):**
  - Sobrecarga del operador `+` (`operator fun plus`) en `Pedido` para combinar dos pedidos fusionando ítems y recalculando totales y descuentos.
  - Sobrecarga del operador `==` (`equals`) en `Producto` para comparar ítems basándose en nombre y categoría.
- **Declaraciones de Desestructuración:**
  - Desestructuración posicional de `Cliente`: `val (nombre, correo, telefono) = cliente`.
  - Desestructuración posicional de `Pedido`: `val (cliente, productos, total) = pedido`.
- **Evaluación de Igualdad y Control de Duplicados:**
  - Implementación coherente de `equals()` y `hashCode()` en `Cliente` y `Producto`.
  - Prevención activa de registros duplicados en colecciones de tipo `Set` / `HashSet`.

---

## 🚀 Compilación y Ejecución

Compilación mediante el compilador de Kotlin (`kotlinc`):

```bash
kotlinc Main.kt -include-runtime -d App.jar
java -jar App.jar
```
