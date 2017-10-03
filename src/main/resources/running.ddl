create dataverse feeds if not exists;
use dataverse feeds;
drop dataset Features if exists;
drop type FeatureType if exists;
create type FeatureType as closed {
  fid: string,
  outlook: string,
  temperature: double,
  humidity: double,
  windy: string,
  play: string
}

create dataset Features(FeatureType)
primary key fid;

create feed mlfeed using "mllib#data_mining" (("server"="127.0.0.1"),
("data-file"="/Volumes/USBStorage/dataset/weather.txt"), ("type-name"="FeatureType"));
connect feed mlfeed to dataset Features;

# TEST
create dataverse weka if not exists;
use dataverse weka;
drop dataset ClassifiedTweets if exists;
drop type TextType if exists;
create type TextType as open {
tweetid:int64, text:string
}
create dataset TweetData(TextType)
primary key tweetid;

create type CSVTextType as closed {
tweetid:int64, text:string, class:string
}

# CSV Import
use dataverse weka;
drop dataset  TweetData if exists;
create dataset TweetData(TextType)
primary key tweetid;
load dataset "TweetData" using localfs
(("path"="127.0.0.1:///Users/heri/Work/Projects/AsterixDB/Dataset/tweet_sentiment_trainingandtestdata/weka_test.csv"),
("format"="delimited-text"));

for $t in dataset TweetData
return $t;

for $t in dataset TweetData
return mllib#classifyText($t);

#SQL++
use twitter;
SELECT `mllib#classifyText`(t) from TweetData t;
SELECT t from TweetData t;


# Feeds
create feed testtyped using "mllib#test_typed" (("num_output_records"="10"), ("type-name"="TextType"));
connect feed testtyped to dataset TweetData;

# Export to CSV
use dataverse weka;
drop dataset TweetFeatures if exists;
drop type FeatureType if exists;
create type FeatureType as open {
}
for $t in dataset TweetData
return mllib#extractFeatures($t);


# Extract raw data
curl -G "http://localhost:19002/query" \
    --data-urlencode 'output=CSV' \
    --data-urlencode 'query=use dataverse weka;
set output-record-type "TextType";
for $t in dataset TweetData
return $t;'

# Extract features
curl -G "http://localhost:19002/query" -H "Accept: text/csv; header=present"\
    --data-urlencode 'query=use dataverse weka;
set output-record-type "FeatureType";
for $t in dataset TweetData
return mllib#extractFeatures($t);'

curl -v --data-urlencode "statement=use weka;
                                                                SELECT t.text as tweet,
                                                                `mllib#extractFeatures`(t) as features
                                                                FROM  TweetData t;" \
          --data pretty=true                     \
          --data client_context_id=xyz           \
          http://localhost:19002/query/service


curl -G "http://localhost:19002/query" \
    --data-urlencode 'output=JSON' \
    --data-urlencode 'query=use weka;
                            SELECT t.text as tweet,
                            `mllib#extractFeatures`(t) as features
                            FROM  TweetData t;'