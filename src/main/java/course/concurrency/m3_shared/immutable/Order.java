package course.concurrency.m3_shared.immutable;

import java.util.List;
import java.util.stream.Collectors;

import static course.concurrency.m3_shared.immutable.Order.Status.NEW;

public final class Order {

    public enum Status { NEW, IN_PROGRESS, DELIVERED }

    private final Long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    private Order(Builder builder) {
        this.id = builder.id;
        this.items = builder.items;
        this.paymentInfo = builder.paymentInfo;
        this.isPacked = builder.isPacked;
        this.status = builder.status;
    }

    public static Builder newWithItems(List<Item> items) {
        return new Builder(items.stream().map(item -> new Item()).collect(Collectors.toList()));
    }

    public synchronized boolean checkStatus() {
        return items != null && !items.isEmpty() && paymentInfo != null && isPacked;
    }

    public Long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items.stream().map(item -> new Item()).collect(Collectors.toList());
    }

    public PaymentInfo getPaymentInfo() {
        return paymentInfo;
    }

    public Order withPaymentInfo(PaymentInfo paymentInfo) {
        return this.toBuilder()
                .paymentInfo(paymentInfo)
                .status(Status.IN_PROGRESS)
                .build();
    }

    private Builder toBuilder() {
        return new Builder(this.items)
                .id(this.id)
                .packed(this.isPacked)
                .status(this.status)
                .paymentInfo(this.paymentInfo);
    }
    
    public boolean isPacked() {
        return isPacked;
    }

    public Order withPacked(boolean packed) {
        return this.toBuilder()
                .packed(packed)
                .status(Status.IN_PROGRESS)
                .build();
    }

    public Status getStatus() {
        return status;
    }

    public Order withStatus(Status status) {
        return this.toBuilder()
                .status(status)
                .build();
    }
    
    public static class Builder {
        private Long id;
        private final List<Item> items;
        private PaymentInfo paymentInfo;
        private boolean isPacked;
        private Status status;
        
        public Builder(List<Item> items) {
            this.items = items;
            this.status = NEW;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder paymentInfo(PaymentInfo paymentInfo) {
            this.paymentInfo = paymentInfo;
            return this;
        }

        public Builder packed(boolean packed) {
            this.isPacked = packed;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }
}
