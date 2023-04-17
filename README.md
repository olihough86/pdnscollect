# pdnscollect

Browser extension and local listener PoC for collecting your own DNS data while browsing

## Listen

This is not a finished working project you can clone and run. If you don't know how to load a dev Chrome extension or if you know nothing about Go or how to post data to an API, this is not for you.

Think of a very small number; it's that version. Security? lol

If you think it's a cool concept though... I WANT TO HEAR FROM YOU! Go mad, create PRs. I'll be on this all day at least until something that works pops out of the other end.

Avoid adding too much logic or processing in the extension; it needs to be lightweight. The poor thing will have a Tor client stuffed into it soon enough, and that will make it sad. Enrichment should be done on the API side.

I foresee a bug that may arise after being open for a long time. The array of already submitted hosts used to avoid too many repeat submissions will grow very large; this needs to be redone.

## Updates from the front line

### 2023.04-16 04:00

I'm trying to fix the messy go layout, there is now a proper moduel refrenced to gihub.com/olihoughio/pdnscollect/api not sure if it's broken or not if so just figure it out or fix it and make a PR, it's not really needed but standards innit, sorting this out will save dep headaches in the future.

### 2023-04-14 13:00
Between coffee number 1 and coffee number 3, these things happened:

1. A basic filter list was added to the extension (right-click "options"). You can put words in there like your name or workplace. Any host containing those words will be dropped before being sent to the API. Feel free to improve this and submit a PR.
2. Elasticsearch is now required for storage. Install Elastic 8.x (latest), bind it to 127.0.0.1:9200, and turn off all the xpack security (or improve main.go to support it). You can use Kibana to look at your data with ease.
3. A UTC timestamp is passed with each log. This is Elastic compatible, yay! Select it when creating your index pattern. There is no longer an ID field passed; Elastic will create one by magic.

Yes, this whole stack will run on a Raspberry Pi 4, just barely. RAM is your problem.
