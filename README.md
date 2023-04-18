# pdnscollect

Browser extension, Android app and API endpoint PoC for collecting your own DNS data while browsing and logging it to Elastic

## Listen

This is not a finished working project you can clone and run. If you don't know how to load a dev extension, run a unpackaged Android app or if you know nothing about Go or how to post data to an API, this is not for you.

Think of a very small number; it's that version. Security? lol

If you think it's a cool concept though... I WANT TO HEAR FROM YOU! Go mad, create PRs. I'll be on this all day at least until something that works pops out of the other end.

Avoid adding too much logic or processing in the extension; it needs to be lightweight. The poor thing will have a Tor client stuffed into it soon enough, and that will make it sad. Enrichment should be done on the API side.

I foresee a bug that may arise after being open for a long time. The array of already submitted hosts used to avoid too many repeat submissions will grow very large; this needs to be redone.

## This is not an install guide

have the lastest 8.x elastic on your machine (use ssh tunnels if it's elsewhere) disable xpack, ssl etc
bind elastic to 127.0.0.1:9200
clone repo
install golang
install the required go deps (look in main.go)
build and run or just run main.go it is now listening on 127.0.0.1:8080

load correct ext from ext/

browse interwebs

data is in elastic, you should also see output from main.go

Don't for the love of all that is pure expose the API to the Internet it will be bad news bears.

## Updates from the front line

### 2023-04-18 16:37

Merged Android branch into main it's a horror show but it works on my emulator

### 2023-04-18 15:55

After a rather tedious fight with Android, the Android branch is here, a WIP with very hardcoded strings and a bunch of usless boilerplate and WTF stuff, but... it does parse out the hostnames from the mutant Andoid DNS requsts, it does not resolve them so it leaves the IP field blank, I'm working on it. It seems to work with all apps from what I can see, as it routes the device traffic through a local VPN allowing it to capture packets. I have been turning PrivateDNS off in settings, I guess you need to also. Will merge into main once the IP field is sorted.

### 2023-04-17 16:00

Chrome support
Firefox support

filtering should work on both, though Chrome is getting a rewrite soon to match up with Firefox, Firefox already drops everything that resolves to 127.0.0.1 or has the hostname localhost by dfault, (yes I know, IPv6, you'll have to wait)

ip_address has been renamed to ip on both ext and api, whoops that's how I broke everything

loading an unpacked ext in Firefox means going to about:debugging loading the ext from there by selecting the manifest.json 
you will find debugging messages in there too by hitting Inspect.

over on the API side, it does a quick check to see if a host has a CNAME if it does it will update the CNAME field if not then it will match the domain, this is fine.

### 2023-04-17 13:52

I broke it and then fixed it *shrug* YOLO dev is best dev, there is FireFox now, I'm fixing Chrome.

### 2023-04-16 04:00

I'm trying to fix the messy go layout, there is now a proper moduel refrenced to gihub.com/olihoughio/pdnscollect/api not sure if it's broken or not if so just figure it out or fix it and make a PR, it's not really needed but standards innit, sorting this out will save dep headaches in the future.

### 2023-04-14 13:00
Between coffee number 1 and coffee number 3, these things happened:

1. A basic filter list was added to the extension (right-click "options"). You can put words in there like your name or workplace. Any host containing those words will be dropped before being sent to the API. Feel free to improve this and submit a PR.
2. Elasticsearch is now required for storage. Install Elastic 8.x (latest), bind it to 127.0.0.1:9200, and turn off all the xpack security (or improve main.go to support it). You can use Kibana to look at your data with ease.
3. A UTC timestamp is passed with each log. This is Elastic compatible, yay! Select it when creating your index pattern. There is no longer an ID field passed; Elastic will create one by magic.

Yes, this whole stack will run on a Raspberry Pi 4, just barely. RAM is your problem.
