(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('FlowDeleteController',FlowDeleteController);

    FlowDeleteController.$inject = ['$uibModalInstance', 'entity', 'Flow'];

    function FlowDeleteController($uibModalInstance, entity, Flow) {
        var vm = this;

        vm.flow = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Flow.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
