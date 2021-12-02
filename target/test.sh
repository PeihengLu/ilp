for i in {1..12}
do
  for j in {1..31}
  do
    java -jar ilp-1.0-SNAPSHOT.jar $j $i 2023 9898 9876
  done
done
