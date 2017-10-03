# asterix-machine-learning
En zip-fil med all rådata kan du finne her: http://207.154.216.178
Det er omtrent 50 millioner Tweets med alle felter tatt vare på. For eksperimentene så lagde jeg subset av denne hvor jeg kun tok vare på tekst-feltet (som beskrevet her). Måtte også gjøre en jobb med å fjerne duplikater, ettersom re-tweets dukker opp flere ganger og ikke differensieres når jeg bare benytter tekst-feltet.

p.s. Det står i dokumentasjonen til asterix-machine-learning at jeg benyttet ID-feltet også (her). Det ble gjort i de første eksperimentene, men ikke de siste. Der ble tekst-feltet brukt som ID når datasettet ble laget i 
AQL ( create type TextType as open { text: string }; create dataset TestDataset(TextType) primary key text; ). Alle felter som gis i input-record settes i output-record.

Bare spør om noe er uklart :)

# Installing a UDF
```
./udf.sh -m [i|u] -d DATAVERSE_NAME -l LIBRARY_NAME [-p UDF_PACKAGE_PATH]
```

Example:
```shell
/Users/heri/asterixdb/opt/ansible/bin/udf.sh -m i -d weka -l mllib -p /Users/heri/git/apache/asterix-machine-learning/target/asterix-external-lib-zip-binary-assembly.zip
   ```
Uninstall:
```
/Users/heri/asterixdb/opt/ansible/bin/udf.sh -m u -d weka -l mllib
``