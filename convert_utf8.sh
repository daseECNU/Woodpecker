#!/bin/bash

find src -type d -exec mkdir -p utf/{} \;
find src -type f -exec iconv -f GBK -t UTF-8 {} -o utf/{} \;

cp -r utf/src .
rm -rf utf