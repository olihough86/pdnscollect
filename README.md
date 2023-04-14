# pdnscollect
Browser extension and local listener PoC for collecting your own DNS data while browsing

## Listen
This is not a finished working project you can clone and run, if you don't know how to load a dev Chrome extension, if you know nothing about Go or how to post data to an API, this is not for you.

Think of a very small number, it's that version. Security? lol

If you think it's a cool concept though... I WANT TO HEAR FROM YOU go mad, create PRs, I'll be on this all day at least until something that works pops out of the other end.

Avoid adding too much logic or processing in the extention, it needs to be lightweight, the poor thing will have a Tor client stufefed into it soon enough and that will make it sad. Enrichment should be done on the API side.

I forsee a bug which may arise after being open a long time, the array of already submitted hosts used to avoid too may repeat submissions will grow very large, this needs to be re done.

## Updates from the front line
2023-14-04 13:00
Between coffee number 1 and coffee number 3 these things happened:
  1. A basic filter list was added to the extension (right click "options") you can put words in there like your name or workplace, any host containing those words will be dropped before being sent to the API, feel free to improve this and submit a PR
  2. Elasticsearch is now required for storage, install elastic 8.x (latest) bind it to 127.0.0.1:9200 and turn off all the xpack security (or improve main.go to support it) you can use Kinbana to look at your data with ease.
  3. A UTC timestamp is passed with each log, this is elastic compatable yey! select it when creating your index pattern. There is no longer an ID field passed, Elasic will creat one by magic.
  
Yes this whole stack will run on a Raspberry Pi 4, just, RAM is your probelm.

