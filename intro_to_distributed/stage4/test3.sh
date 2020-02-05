arr=()
for i in {1..8} #index[7]=gateway, index[6]=leader
do 
	java -cp ./target/classes edu/yu/cs/fall2019/intro_to_distributed/Driver $i & arr+=("$!")
done

sleep 120
pkill java