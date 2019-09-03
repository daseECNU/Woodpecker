#!/bin/bash
loop()
{
	echo -ne "while true
	do i=0;done" | /bin/bash &	
}
for i in `seq $1`
do 
	loop
	pid[$i]=$!
done
sleep $2
for i in "${pid[@]}";
do 
	kill $i;
done
