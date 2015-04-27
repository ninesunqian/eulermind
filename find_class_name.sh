#!/bin/bash
classes=()
for class_path in `find . -name "*.class"`
do
    class_file=`basename $class_path`
    class_name=${class_file/.class/}
    classes[${#classes}]=$class_name
    echo ${classes[@]}
done

echo "you have ${#classes} classes:  ${classes[@]}"

for resource_path in `find . -type f  -a  -not -name "*.class"`
do
    for class in ${classes[@]}
    do
        if grep $class $resource_path -q; then
            echo "file: $resource_path: $class"
        fi
    done
done
