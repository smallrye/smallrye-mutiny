#!/bin/bash
FILE=_data/versions.yml
MUTINY_VERSION=$1
VERTX_CLIENTS_VERSION=$2
echo "Setting Mutiny version to $MUTINY_VERSION"
echo "Setting Vert.x clients version to $VERTX_CLIENTS_VERSION"
if [[ -f "$FILE" ]]
then
    cat <<EOF >$FILE
mutiny_version: $MUTINY_VERSION
vertx_mutiny_clients: $VERTX_CLIENTS_VERSION
EOF
else 
    echo "Cannot find $FILE"    
    exit 1
fi
