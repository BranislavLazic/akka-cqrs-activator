// Tests the ng-lodash module has access to lodash functions as accessible
describe('ng-lodash', function () {

    beforeEach(module('ngLodash'));

    it('should have lodash as a constant defined', inject(function (lodash) {
        expect(lodash).toBeDefined();
    }));

    it('should contain the lodash toArray function', inject(function (lodash) {
        expect(lodash.toArray).toBeDefined();
    }));

    it('should use be able to use a lodash function', inject(function (lodash) {
        var testObject = {
                1: 's',
                2: 'a',
                3: 'd'
            },
            valid = lodash.chain(testObject).toArray().isArray().value();

        expect(valid).toBeTruthy();
    }));

});
