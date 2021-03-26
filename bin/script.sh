#bin/bash
for value in 20_60 005_01 01_5 05_10 20 30 40 50 60
do
bin/tdb2.tdbqueryplan --explain --loc /media/data/NeoSPARQL/watdiv/tdbdata/ --queriesFile "/home/dcasals/jupyter/mlproject/pytorch_watdiv/queries_filtered/afiltrado_${value}.csv" --idColumn 0 --queryColumn 1 --delimiter "ᶶ"
bin/tdb2.qplan_dataset --logFile ~/source/jena-neooptimizer-3.16.0-SNAPSHOT/fuseki_logcomplete.json --outFile "~/jupyter/mlproject/pytorch_watdiv/plandatasetwatdiv_${value}.csv"
rm fuseki_logcomplete.json
done
