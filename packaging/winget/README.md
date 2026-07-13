# Publicación en WinGet

Los manifiestos definitivos se generan cuando exista una URL pública e inmutable para el instalador de la versión publicada.

Recomendación para Ely-Tesia:

1. Publicar primero el instalador en GitHub Releases o Microsoft Store.
2. Usar `wingetcreate new <URL-del-instalador>`.
3. Reservar el identificador `biglexj.ElyTesia`.
4. Comprobar con `winget validate manifests` antes de abrir el PR en `microsoft/winget-pkgs`.

No se debe reutilizar el identificador de paquete de WinTTS. El mismo certificado
de publicación puede firmar más de una aplicación si su sujeto coincide exactamente
con `Publisher`; la identidad de Microsoft Store debe copiarse desde Partner Center.
