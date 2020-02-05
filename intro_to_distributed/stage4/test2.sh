HTTPD=`curl -A "" -sL --connect-timeout 3 -w "%{http_code}\n" "http://localhost:7095/getLeaderAndClusterInfo" -o /dev/null`
curl -s http://localhost:7095/getLeaderAndClusterInfo;
until [ "$HTTPD" == "200" ]; do
    sleep 3
    HTTPD=`curl -A "" -sL --connect-timeout 3 -w "%{http_code}\n" "http://localhost:7095/getLeaderAndClusterInfo" -o /dev/null`
    printf "\n"
    curl -s http://localhost:7095/getLeaderAndClusterInfo;
done

syncPids=()
for i in {0..100}
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
wait
echo "did I get stuff back?"