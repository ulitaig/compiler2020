set -e
cd "$(dirname "$0")"
export CCHK="java -classpath /ulib/java/antlr-4.8-complete.jar:./bin compiler.Main"
cat > code.txt
$CCHK
cp ./test.s output.s