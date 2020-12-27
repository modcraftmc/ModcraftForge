echo "Installing server"

mkdir cleanserver

cp projects/forge/build/libs/forge-1.15.2-31.3.0-installer.jar cleanserver
java -jar cleanserver/forge-1.15.2-31.3.0-installer.jar --installServer cleanserver
while getopts ":c" opt; do
  case ${opt} in
    c ) # process option h

      echo "Installing client"
      java -jar cleanserver/forge-1.15.2-31.3.0-installer.jar

      ;;
    \? ) echo "Usage: cmd [-c]"
      ;;
  esac
done
rm forge-1.15.2-31.3.0-installer.jar.log
cd cleanserver || exit
java -jar -Xmx1G forge-1.15.2-31.3.0.jar nogui
