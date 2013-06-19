while read p; do
  echo $p | awk '{ print("mv templates/" $1 ".ftl templates/" $2 ".ftl; mv references/" $1 ".txt references/" $2 ".txt") }'
done < renames.txt