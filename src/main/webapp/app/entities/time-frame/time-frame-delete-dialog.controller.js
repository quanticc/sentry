(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('TimeFrameDeleteController',TimeFrameDeleteController);

    TimeFrameDeleteController.$inject = ['$uibModalInstance', 'entity', 'TimeFrame'];

    function TimeFrameDeleteController($uibModalInstance, entity, TimeFrame) {
        var vm = this;

        vm.timeFrame = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            TimeFrame.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
