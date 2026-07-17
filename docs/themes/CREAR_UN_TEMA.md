# Cómo crear un tema

## 1. Identidad

Usa un `id` estable, en minúsculas y que no suplante a otro autor, por ejemplo
`com.alex.tema-nocturno`. Incrementa `version` cuando publiques cambios y elige
una licencia explícita.

## 2. Colores

Los colores aceptan `#RRGGBB` y `#RRGGBBAA`. Los campos Material usan roles
semánticos: todo fondo `primary` debe acompañarse con texto `onPrimary`, y así
sucesivamente. Ely-Tesia exige contraste mínimo 4.5:1 en parejas críticas.

Los tokens `leftHand` y `rightHand` colorean notas naturales. Las notas negras
(sostenidos/bemoles) usan `blackKeyPressed`. Si el MIDI no declara manos,
Ely-Tesia intenta usar la pista y después divide por Do central (MIDI 60).

## 3. Efectos

`pressedGlow`, `noteTrail` y `particleIntensity` aceptan valores de `0.0` a
`1.0`. `expressiveMotion` permite movimiento expresivo; el sistema puede
reducirlo si el usuario solicita menos animaciones.

## 4. Prueba

Prueba teclas blancas y negras, ambas manos, aciertos, errores, modo espera,
texto sobre superficies y una pantalla Android pequeña antes de compartir.
