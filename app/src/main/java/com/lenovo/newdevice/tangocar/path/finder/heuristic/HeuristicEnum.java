package com.lenovo.newdevice.tangocar.path.finder.heuristic;

public enum HeuristicEnum implements HeuristicCreator {
    HeuristicSquared {
        @Override
        public HeuristicScheme create() {
            return new HeuristicSquared();
        }
    },
    HeuristicChebyshev {
        @Override
        public HeuristicScheme create() {
            return new HeuristicChebyshev();
        }
    },
    HeuristicDiagonal {
        @Override
        public HeuristicScheme create() {
            return new HeuristicDiagonal();
        }
    },
    HeuristicEuclidean {
        @Override
        public HeuristicScheme create() {
            return new HeuristicEuclidean();
        }
    },
    HeuristicManhattan {
        @Override
        public HeuristicScheme create() {
            return new HeuristicManhattan();
        }
    };
}
