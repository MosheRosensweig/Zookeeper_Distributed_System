#Moshe Rosensweig
exec > >(tee "output.log") 2>&1 #write all printouts to file

#STEP 1 - 
printf "\n[STEP 1]\n"
mvn clean test

#STEP 2 - Create a cluster of 7 nodes and one gateway, starting each in their own JVM
printf "\n[STEP 2]\n"
arr=()
for i in {1..8} #index[7]=gateway, index[6]=leader
do 
	java -cp ./target/classes edu/yu/cs/fall2019/intro_to_distributed/Driver $i & arr+=("$!")
done

#STEP 3 - wait for a leader, and print out all server states when you get one (not sure why it's printing out twice)
printf "\n[STEP 3]\n"
HTTPD=`curl -A "Web Check" -sL --connect-timeout 3 -w "%{http_code}\n" "http://localhost:7095/getLeaderAndClusterInfo" -o /dev/null`
until [ "$HTTPD" == "200" ]; do
    sleep 3
    HTTPD=`curl -A "" -sL --connect-timeout 3 -w "%{http_code}\n" "http://localhost:7095/getLeaderAndClusterInfo" -o /dev/null`
    printf "\n"
    curl -s http://localhost:7095/getLeaderAndClusterInfo
done
sleep 3
#STEP 4 -
printf "\n[STEP 5]\n"
for i in {0..8}
do
	printf "The request code = \n"
	reqStr='public class SampleClass_t {
	    public void run() {
	        System.out.println( "SampleClass has been compiled and run!! Test number '$i'" );
	        //System.err.println("I am not a fish man, stop saying that!");
	         //throw new RuntimeException("You are a geek");
	         //throw new NullPointerException("blaaaaaaaah");
	    }
	}'
	echo "$reqStr"
	printf "\n The resonpose =\n"
	curl -s http://localhost:7095/compileandrun -d "$reqStr"
	printf "\n\n";
done
#STEP 5 -
printf "\n[STEP 5]\n" 
echo "Time to kill FOLLOWER #6"
kill -9 "${arr[5]}"
sleep 8s
curl -s http://localhost:7095/getLeaderAndClusterInfo

#STEP 6 - 
printf "\n[STEP 6]\n"
echo "Time to kill the leader id#7"
kill -9 "${arr[6]}"
sleep 0.5
syncPids=()
for i in {0..8}
do
	printf "The request code = \n"
	reqStr='public class SampleClass_t {
	    public void run() {
	        System.out.println( "SampleClass has been compiled and run!! Test number '$i'" );
	        //System.err.println("I am not a fish man, stop saying that!");
	         //throw new RuntimeException("You are a geek");
	         //throw new NullPointerException("blaaaaaaaah");
	    }
	}'
	echo "$reqStr"
	curl -s http://localhost:7095/compileandrun -d "$reqStr" & syncPids+=($!)
	printf "\n\n";
done
#STEP 7 -
printf "\n[STEP 7]\n" 
sleep 8 #election time 
HTTPD=`curl -A "" -sL --connect-timeout 3 -w "%{http_code}\n" "http://localhost:7095/getLeaderAndClusterInfo" -o /dev/null`
curl -s http://localhost:7095/getLeaderAndClusterInfo;
until [ "$HTTPD" == "200" ]; do
    sleep 3
    HTTPD=`curl -A "" -sL --connect-timeout 3 -w "%{http_code}\n" "http://localhost:7095/getLeaderAndClusterInfo" -o /dev/null`
    printf "\n"
    curl -s http://localhost:7095/getLeaderAndClusterInfo;
done
#STEP 7.5 - 
echo "Seeing if PIDS exist"
for pid in "${syncPids[@]}"; do
   wait "$pid"
   echo "Pid $pid is terminated"
   # do something when a job completes
done
#STEP 8 - 
printf "\n[STEP 8]\n"
for i in {0..2}
do
	printf "The request code = \n"
	reqStr='public class SampleClass_t {
	    public void run() {
	        System.out.println( "SampleClass has been compiled and run!! Test number '$i'" );
	        //System.err.println("I am not a fish man, stop saying that!");
	         //throw new RuntimeException("You are a geek");
	         //throw new NullPointerException("blaaaaaaaah");
	    }
	}'
	echo "$reqStr"
	printf "\n The resonpose =\n"
	curl -s http://localhost:7095/compileandrun -d "$reqStr"
	printf "\n\n";
done
#STEP 9 - 
printf "\n[STEP 9]\n"
printf "Killing all java servers"

sleep 2s
echo "killing java"
pkill java



