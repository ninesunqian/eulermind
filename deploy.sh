cp -f utils/jnlps/*.jnlp eulermind/target/jnlp/ 
rsync -av eulermind/target/jnlp/ /home/wangxuguang/ninesunqian.github.io/jnlp/
pushd /home/wangxuguang/ninesunqian.github.io
git commit -a -m "update in `date --rfc-3339=seconds`"
popd
