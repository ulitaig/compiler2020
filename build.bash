set -e
cd "$(dirname "$0")"
mkdir -p bin
find ./src/compiler -name *.java | javac  -d bin -classpath "/ulib/java/antlr-4.8-complete.jar" @/dev/stdin