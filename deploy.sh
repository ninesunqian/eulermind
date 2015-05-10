#!/bin/bash

cp utils/eulermind.png eulermind/target/jnlp/

pushd utils/
python modify_jnlp.py
popd

rsync -av eulermind/target/jnlp/ /home/wangxuguang/ninesunqian.github.io/jnlp/
pushd /home/wangxuguang/ninesunqian.github.io
git add jnlp/*
git commit -a -m "update in `date --rfc-3339=seconds`"
git push origin master
popd
