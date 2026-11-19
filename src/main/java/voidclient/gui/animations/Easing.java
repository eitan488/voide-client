package voidclient.gui.animations;

/** Standard easing curves, t in [0,1]. */
public enum Easing {
    LINEAR {
        public double apply(double t) { return t; }
    },
    SINE_OUT {
        public double apply(double t) { return Math.sin(t * Math.PI / 2); }
    },
    QUAD_OUT {
        public double apply(double t) { return 1 - (1 - t) * (1 - t); }
    },
    CUBIC_OUT {
        public double apply(double t) { return 1 - Math.pow(1 - t, 3); }
    },
    CUBIC_IN_OUT {
        public double apply(double t) { return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2; }
    },
    QUINT_OUT {
        public double apply(double t) { return 1 - Math.pow(1 - t, 5); }
    },
    EXPO_OUT {
        public double apply(double t) { return t >= 1 ? 1 : 1 - Math.pow(2, -10 * t); }
    },
    BACK_OUT {
        public double apply(double t) {
            double c1 = 1.70158, c3 = c1 + 1;
            return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
        }
    },
    ELASTIC_OUT {
        public double apply(double t) {
            if (t <= 0 || t >= 1) return t <= 0 ? 0 : 1;
            return Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1;
        }
    };

    public abstract double apply(double t);
}
