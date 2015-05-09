function make_jnlp ()
{
    new_jnlp="$1.jnlp"
    host="$2"

    sed 's/${project.url}/project_url/g' launch.jnlp >  $new_jnlp

    sed -i "s/project_url/http:\/\/$host/g"                      $new_jnlp
    sed -i "s/launch.jnlp/$new_jnlp/g"                  $new_jnlp
    sed -i '/<\/title>/a <vendor>eulermind</vendor>'    $new_jnlp
}

pushd eulermind/target/jnlp/

make_jnlp localhost localhost
make_jnlp eulermind www.eulermind.com
make_jnlp ali 123.57.204.59

popd

rsync -av eulermind/target/jnlp/ /home/wangxuguang/ninesunqian.github.io/jnlp/
