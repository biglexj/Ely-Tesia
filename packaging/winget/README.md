# Publicación en WinGet

Los manifiestos de la versión 1.0.2 están en
`manifests/b/biglexj/ElyTesia/1.0.2` y utilizan el instalador MSI publicado en
GitHub Releases.

Recomendación para Ely-Tesia:

1. Publicar primero el MSI en GitHub Releases.
2. Confirmar que la URL sea pública y que su SHA-256 coincida.
3. Comprobar con `winget validate --manifest <directorio>`.
4. Copiar el árbol desde `manifests/` al fork de `microsoft/winget-pkgs`.
5. Abrir un PR para reservar el identificador `biglexj.ElyTesia`.

No se debe reutilizar el identificador de paquete de WinTTS. El mismo certificado
de publicación puede firmar más de una aplicación si su sujeto coincide exactamente
con `Publisher`; la identidad de Microsoft Store debe copiarse desde Partner Center.
