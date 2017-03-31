package com.lenovo.newdevice.tangocar.path.finder.neighbor;

public enum NeighborEnum implements NeighborCreator {

    NeighborFourDirections {
        @Override
        public NeighborSelector create() {
            return new NeighborFourDirections();
        }
    },

    NeighborEightDirections {
        @Override
        public NeighborSelector create() {
            return new NeighborEightDirections();
        }
    },

    NeighborJumpPoint {
        @Override
        public NeighborSelector create() {
            return new NeighborJumpPoint();
        }
    };
}
