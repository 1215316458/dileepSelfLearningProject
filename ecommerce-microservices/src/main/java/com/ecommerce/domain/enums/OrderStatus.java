package com.ecommerce.domain.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {

    PENDING {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Collections.unmodifiableSet(EnumSet.of(CONFIRMED, CANCELLED));
        }
    },
    CONFIRMED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Collections.unmodifiableSet(EnumSet.of(SHIPPED, CANCELLED));
        }
    },
    SHIPPED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            return Collections.unmodifiableSet(EnumSet.of(DELIVERED));
        }
    },
    DELIVERED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            // terminal state — no further transitions allowed
            return Collections.emptySet();
        }
    },
    CANCELLED {
        @Override
        public Set<OrderStatus> allowedTransitions() {
            // terminal state — no further transitions allowed
            return Collections.emptySet();
        }
    };

    // each status defines its own valid next states
    public abstract Set<OrderStatus> allowedTransitions();

    public boolean canTransitionTo(OrderStatus next) {
        return allowedTransitions().contains(next);
    }
}
