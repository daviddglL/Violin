# Violin Master Pro

Compañero de práctica de violín para Android. Una app que reúne las herramientas esenciales para violinistas de todos los niveles: afinador, metrónomo, seguimiento de práctica, plan de estudios y comunicación profesor-alumno.

## Qué hace

- **Afinador inteligente** — detecta la cuerda automáticamente y te guía hasta la afinación perfecta con feedback visual en tiempo real.
- **Metrónomo** — pulso rítmico configurable con acentos de compás y tap tempo.
- **Registro de práctica** — cronometra tus sesiones, mide tu progreso diario y visualiza tu evolución semanal.
- **Plan de estudios** — lecciones guiadas por niveles (Principiante, Intermedio, Avanzado) con ejercicios de arco, digitación y teoría.
- **Panel profesor-alumno** — los profesores asignan tareas con videos demostrativos; los alumnos las reciben, practican y marcan como completadas.
- **Chat integrado** — comunicación directa entre profesor y alumno dentro de la app.
- **Sistema de logros** — ganá puntos por completar ejercicios y tareas, competí en la tabla de clasificación global.
- **Modo seguro para menores** — detección y difuminado facial automático en videos para proteger la identidad de estudiantes menores de 18.

## Roles de usuario

| Rol | Descripción |
|-----|-------------|
| **Profesor (Teacher)** | Crea tareas, adjunta videos, gestiona alumnos vinculados mediante código de invitación. |
| **Estudiante (Student)** | Se vincula a un profesor, recibe tareas, completa ejercicios, chatea con su instructor. |
| **Freelancer** | Modo autodidacta. Accede a todo el plan de estudios sin necesidad de profesor. |

## Lo que se pretende conseguir

El objetivo es que cualquier violinista — estudie con profesor o por su cuenta — tenga en el bolsillo un conjunto completo de herramientas para:

- Mantener la disciplina de práctica diaria con métricas reales.
- Afinar con precisión sin depender de oído absoluto.
- Seguir un plan de estudios estructurado que cubre técnica, teoría y repertorio.
- Recibir feedback y tareas personalizadas de su profesor en tiempo real.
- Construir un historial de progreso medible semana a semana.

En esencia: **que la tecnología no reemplace al profesor ni al instrumento, sino que potencie las horas de práctica con datos, estructura y motivación.**

## Tecnología

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Arquitectura**: Clean Architecture + MVVM con Hilt (DI)
- **Base de datos local**: Room (SQLite)
- **Autenticación**: PIN de 4 dígitos + cifrado hash/sal + Google Sign-In (Firebase Auth)
- **Cloud**: Firebase Firestore + Storage (chat, videos, asignaciones)
- **Testing**: JUnit + Robolectric + Roborazzi (screenshot testing)

## Requisitos para desarrollar

- Android Studio Hedgehog o superior
- JDK 17+
- Clave de API de Gemini (archivo `.env` en la raíz del proyecto, ver `.env.example`)

## Ejecutar

1. Cloná el repo y abrilo en Android Studio.
2. Creá un archivo `.env` con tu `GEMINI_API_KEY`.
3. Sincronizá Gradle y ejecutá en un emulador o dispositivo físico (API 24+).
