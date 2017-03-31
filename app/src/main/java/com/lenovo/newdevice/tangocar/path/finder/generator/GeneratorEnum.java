package com.lenovo.newdevice.tangocar.path.finder.generator;

public enum GeneratorEnum implements GeneratorCreator {
    GeneratorRectangle {
        @Override
        public MapGenerator create() {
            return new GeneratorRectangle();
        }
    },
    GeneratorEllipse {
        @Override
        public MapGenerator create() {
            return new GeneratorEllipse();
        }
    },
    GeneratorLines {
        @Override
        public MapGenerator create() {
            return new GeneratorLines();
        }
    },
    GeneratorRandom {
        @Override
        public MapGenerator create() {
            return new GeneratorRandom();
        }
    },
    GeneratorPerfectMaze {
        @Override
        public MapGenerator create() {
            return new GeneratorPerfectMaze();
        }
    };
}
