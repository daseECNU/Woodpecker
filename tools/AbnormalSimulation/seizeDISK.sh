#/bin/bash
count=$1
size=$[$2*1024*1024]
Time=$3

for i in `seq $Time`
do
echo -ne "
 dd if=/dev/zero of=~/disk$i bs=$[$size/$count]K count=$count "  | /bin/bash &
sleep 1
done
for i in `seq $Time`
do
rm ~/disk$i
done
