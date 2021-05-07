### Module to extract SPARQL query features from the Query execution Plan.

Este módulo está desarrollado para extraer características del plan
de ejecución de una consulta SPARQL.
Es un módulo del proyecto Jena pensado para ejecutarse de manera individual al projecto Jena.
Sin embargo, fue necesario algunas modificaciones de otros módulos de Jena, por lo que la mejor forma
de utilizar en este momento es usando el compactado siguiente:

#### Install and Compile

Use this to install dependencies and avoid Jena rules for snapshots:

`mvn clean install -Denforcer.skip=true -Drat.skip=true
`

Use this to package the project and distribute:

`mvn clean package -Denforcer.skip=true -Drat.skip=true
`

### Usage

After package the project:

- uncompress the tar.gz.
- run the bin/tdb2.tdbqueryplan script to get the trees.

#### Example

`
./tdb2.tdbqueryplan --tdb_tree --explain --loc /path/to/tdb2_db --queriesFile /path/to/queries file.csv --idColumn 0 --queryColumn 1 --delimiter "ᶶ" --outFile /path/to/outputprefix_file_
`

- The --loc path indicate the TDB2 database path.
- The --queriesFile indicate a csv file which contain a query and id columns.
- The --delimiter indicate the delimiter character to separate columns.
- The --outFile indicate the path and prefix of output.
