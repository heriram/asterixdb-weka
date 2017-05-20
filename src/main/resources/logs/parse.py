from __future__ import division
from collections import defaultdict
from pprint import pprint
import sys

if len(sys.argv) != 2:
	print('Usage: number of nodes as first parameter')
	sys.exit(1)

NODES = int(sys.argv[1])


def read_log():
	d = defaultdict(dict)
	entry = 0
	f = open('tps.log', 'r')
	for line in f:
		raw_entry = line.rstrip().split(',')
		d[entry]['time'] = int(raw_entry[0])
		d[entry]['count'] = int(raw_entry[1])
		entry += 1
	f.close()
	return d


def total_count(d):
	total_count = 0
	for key in d.keys():
		total_count += d[key]['count']
	return total_count


def reduce_log(d, nodes=2):
	e = defaultdict(dict)
	new_key = 0
	for key in d.keys():
		if key % nodes == 0:
			count = 0
			for i in range(0,nodes):
				count += d[key+i]['count']
			e[new_key]['time'] = d[key]['time']
			e[new_key]['count'] = count
			new_key += 1
	return e


def add_stats(d):
	d[0]['agg_count'] = 0
	for key in d.keys()[1:]:
		time_delta = (d[key]['time'] - d[key-1]['time']) / 1000
		d[key]['delta_t'] = time_delta
		calculated_tps = int(round(d[key]['count'] / time_delta))
		d[key]['agg_count'] = d[key]['count'] + d[key-1]['agg_count']
		d[key]['tps'] = calculated_tps
	d[0]['tps'] = 0
	return d


def calculate_tps(d):
	total_tps = 0
	for key in d.keys():
		total_tps += d[key]['tps']
	avg_tps = int(round(total_tps / len(d)))
	return avg_tps


def write_log(d):
	out = open("tps.dat", "w")
	out.write("time_ms\ttime_s\tcount\tagg_count\ttps\tavg_tps\n")
	for key in d:
		time_ms = d[key]['time']
		time_s = int(round(time_ms/1000))
		count = d[key]['count']
		agg_count = d[key]['agg_count']
		tps = d[key]['tps']
		log_entry = "%s\t%s\t%s\t%s\t%s\t%s\n" % (time_ms,time_s,count,agg_count,tps,avg_tps)
		out.write(log_entry)
	out.close()


d = read_log()
total_count = total_count(d)
print('Total count: ' + str(total_count))
d = reduce_log(d,NODES)
d = add_stats(d)
avg_tps = calculate_tps(d)
print('Average TPS: ' + str(avg_tps))
write_log(d)