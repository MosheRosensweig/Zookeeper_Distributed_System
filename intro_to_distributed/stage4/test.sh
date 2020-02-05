a = 1


while [a != 8]; do a++;

HTTPD=`curl -A "Web Check" -sL --connect-timeout 3 -w "%{http_code}\n" "http://127.0.0.1" -o /dev/null`
until [ "$HTTPD" == "200" ]; do
    printf '.'
    sleep 3
    service nginx restart
    HTTPD=`curl -A "Web Check" -sL --connect-timeout 3 -w "%{http_code}\n" "http://127.0.0.1" -o /dev/null`
done

HTTPD=curl -s https://localhost:GATEWAY_PORT(7095)/getLeader
until [ "$HTTPD" == "200" ]; do
    sleep 2s
    HTTPD=curl -s https://localhost:GATEWAY_PORT(7095)/getLeader
done


HTTPD=1
until [ "$HTTPD" == "2" ]; do
    sleep 2s
    HTTPD =  $HTTPD + 1
done



response=curl -s http://localhost:7095/getLeaderAndClusterInfo
until [ "$response" == "200" ]; do
    sleep 2
    echo $response
    printf "\n"
    hellow=curl -s http://localhost:7095/getLeaderAndClusterInfo
done


working!

HTTPD=`curl -A "Web Check" -sL --connect-timeout 3 -w "%{http_code}\n" "http://localhost:7095/getLeaderAndClusterInfo" -o /dev/null`
until [ "$HTTPD" == "200" ]; do
    sleep 3
    HTTPD=`curl -A "" -sL --connect-timeout 3 -w "%{http_code}\n" "http://localhost:7095/getLeaderAndClusterInfo" -o /dev/null`
done




#STEP 4 - 
for i in {0..8}
do
	printf "The request code = "
	reqStr='
	public class SampleClass_t {
	    public void run() {
	        System.out.println( "SampleClass has been compiled and run!! Test number '$i'" );
	        //System.err.println("I am not a fish man, stop saying that!");
	         //throw new RuntimeException("You are a geek");
	         //throw new NullPointerException("blaaaaaaaah");
	    }
	}'
	echo "$reqStr"
	printf "\n The resonpose =\n"
	curl -s http://localhost:7095/compileandrun -d "$reqStr";
	printf "\n\n"
done
