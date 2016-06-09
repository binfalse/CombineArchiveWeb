#!/usr/bin/python3

import os, sys, json, time, math
import requests

# settings
url = "http://localhost:8080/CombineArchiveWeb/rest/v1/stats"
secret = "1234"
timeout = 0.02

# trying to establish connection
try:
	req = requests.get(url, params=[("secret", secret)], timeout=timeout)
	stats = req.json()
except requests.exceptions.ConnectionError as err:
	print( "Connection Error: {0}".format(err) )
	sys.exit(2)
except requests.exceptions.HTTPError as err:
	print( "HTTP Error: {0}".format(err) )
	sys.exit(2)
except requests.exceptions.Timeout as err:
	print( "Timeout of {1} exceeded: {0}".format(err, timeout) )
	sys.exit(2)
except requests.exceptions.TooManyRedirects as err:
	print( "Too many redirects while requesting stats: {0}".format(err) )
	sys.exit(2)
except ValueError as err:
	print( "Error decoding json: {0}".format(err) )
	sys.exit(2)
except:
	print( "Unknown exception: {0}".format(sys.exc_info()[0]) )
	sys.exit(2)
	
if 'maxStatsAge' and 'generated' in stats:
	now = int( time.time() ) * 1000
	if math.fabs( now - int(stats['generated']) ) >= int(stats['maxStatsAge']) * 1000 :
		print( "Stats are too old. Is System running?" )
		sys.exit(1)

if 'totalSizeQuota' in stats:
	if stats['totalSizeQuota'] >= 0.90: 
		print("Total quoata is about to exceed: {0}%".format(stats['totalSizeQuota']*100) )
		sys.exit(1)
	elif stats['totalSizeQuota'] >= 0.99:
		print( "Total quota is exceeded: {0}%".format(stats['totalSizeQuota']*100) )
		sys.exit(2)
	else:
		print("quota is ok: {0}% ".format(stats['totalSizeQuota']*100) )
		sys.exit(0)
else:
	sys.exit(0)


# no normal ending
print( "script terminated unexpected" )
sys.exit(2)
