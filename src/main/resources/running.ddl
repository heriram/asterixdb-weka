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
create dataverse feeds if not exists;
use dataverse feeds;
drop dataset Features if exists;
drop type FeatureType if exists;
create type TTType as open {
tweetid:int64, message-text:string
}
create dataset TTDS(TTType)
primary key tweetid;
create feed testtyped using "mllib#test_typed" (("num_output_records"="10"), ("type-name"="TTType"));
connect feed testtyped to dataset TTDS;