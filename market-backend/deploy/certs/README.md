# Llaves JWT de producción

`prod-private.pem` / `prod-public.pem` son un par RSA-2048 nuevo, generado
para este deploy — nunca son los mismos que
`src/main/resources/certs/dev-*.pem` (esos son solo para desarrollo local).

Generados con:

```
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out prod-private.pem
openssl rsa -pubout -in prod-private.pem -out prod-public.pem
```

**No subir `prod-private.pem` a ningún repositorio.** `docker-compose.yml`
monta esta carpeta como volumen de solo lectura en `/certs` dentro del
contenedor del backend. Si se rota la llave, todos los refresh tokens y
access tokens emitidos con la anterior dejan de validar — coordinar la
rotación con un reinicio donde se acepte que las sesiones activas se
cierren.
