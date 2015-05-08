

cd eulermind/target/jnlp/



function make_jnlp ()
{
    new_jnlp=""$1.jnlp

    sed 's/${project.url}/http:\/\/localhost/g' launch.jnlp > $new_jnlp
    sed -i 's/launch.jnlp/localhost.jnlp/g' $new_jnlp
    sed -i '/<\/title>/a <vendor>eulermind</vendor>' $new_jnlp
    sed -i '/<\/title>/a <vendor>eulermind</vendor>' $new_jnlp
}

make_jnlp localhost
make_jnlp eulermind
make_jnlp 123.57.204.59
