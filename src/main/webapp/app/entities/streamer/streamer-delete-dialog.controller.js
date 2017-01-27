(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('StreamerDeleteController',StreamerDeleteController);

    StreamerDeleteController.$inject = ['$uibModalInstance', 'entity', 'Streamer'];

    function StreamerDeleteController($uibModalInstance, entity, Streamer) {
        var vm = this;

        vm.streamer = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            Streamer.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
