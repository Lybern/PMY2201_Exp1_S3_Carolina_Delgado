# PMY2201_Exp1_S3_Carolina_Delgado

**Asignatura:** Desarrollo de Aplicaciones Móviles I (PMY2201)  
**Evaluación:** Sumativa Individual - Semana 3 (Experiencia 1): *"Creando soluciones con Kotlin"*  
**Alumna:** Carolina Delgado  
**Institución:** Duoc UC  

---

## 📌 Descripción del Proyecto

Sistema de gestión para una pizzería desarrollado en **Kotlin**, correspondiente a la evolución y refactorización avanzada del proyecto base de la Semana 2. 

El proyecto incorpora utilidades avanzadas de Kotlin:
- **Regex y Ranges (Paso 1):**
  - Validación interactiva de correo electrónico (`nombre@dominio.com`) con solicitud de reingreso ante error.
  - Formateo uniforme de números telefónicos internacionales (`+XX (XXX) XXX-XXXX`) mediante expresiones regulares y grupos de captura.
  - Rango de fechas promocionales (`ClosedRange<LocalDate>`) con aplicación de descuentos automáticos al total.
  - Validación de cantidades permitidas (`1..100`) para pedidos e insumos.
- **Anotaciones y Reflection (Paso 2):**
  - Anotación personalizada `@Promocionable` para catalogar productos especiales.
  - Inspección dinámica mediante Reflection (`KClass`, propiedades y miembros).
- **Sobrecarga de Operadores (Paso 3):**
  - Operador `+` en `Pedido` para combinar pedidos y recalcular totales.
  - Operador `==` en `Producto` comparando por nombre y categoría.
- **Declaraciones de Desestructuración (Paso 4):**
  - Desestructuración de objetos `Cliente` y `Pedido`.
- **Evaluaciones de Igualdad (Paso 5):**
  - Implementación de `equals()` y `hashCode()` para prevenir duplicados en colecciones.
- **Resumen y Demostración en Consola (Paso 6):**
  - Ejecución integral y modular demostrando todas las capacidades operando en conjunto.

---

## 🚀 Compilación y Ejecución

Compilación mediante el compilador oficial de Kotlin (`kotlinc`):

```bash
kotlinc Main.kt -include-runtime -d App.jar
java -jar App.jar
```
