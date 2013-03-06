./xmuta example.pom \
    '\$\{Version\}'    '/:project/:properties/m:manifest/m:version' \
    '\$\{Created-By\}' '/:project/:properties/m:manifest/m:created-by' \
    '\$\{Main-Class\}' '/:project/:properties/m:manifest/m:main-class' \
    < example.manifest.in
