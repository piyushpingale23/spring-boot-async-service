#!/bin/bash

#URL="http://localhost:3001/hello"
#URL="http://localhost:3001/products-sync"
URL="http://localhost:3001/products-async"

START_TIME=$(date +%s%N)

for i in {1..100}
do
    curl -s "$URL" > /dev/null &
done

wait

END_TIME=$(date +%s%N)

ELAPSED_NS=$((END_TIME - START_TIME))
ELAPSED_MS=$((ELAPSED_NS / 1000000))

echo "======================================"
echo "100 requests completed"
echo "Total time: ${ELAPSED_MS} ms"
echo "Total time: $(awk "BEGIN {printf \"%.3f\", $ELAPSED_MS/1000}") seconds"
echo "======================================"