package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
        this.latestBid = new Bid(-1L, -1L, -1L);
    }

    private volatile Bid latestBid;
    private final Object lock = new Object();
    private volatile Boolean running = true;

    public boolean propose(Bid bid) {
        if (running && (bid.getPrice() > latestBid.getPrice())) {
            synchronized (lock) {
                if (running && (bid.getPrice() > latestBid.getPrice())) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    public Bid stopAuction() {
        synchronized (lock) {
            running = false;
            return latestBid;
        }
    }
}
