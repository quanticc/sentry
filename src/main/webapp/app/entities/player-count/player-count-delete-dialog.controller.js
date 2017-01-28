(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PlayerCountDeleteController',PlayerCountDeleteController);

    PlayerCountDeleteController.$inject = ['$uibModalInstance', 'entity', 'PlayerCount'];

    function PlayerCountDeleteController($uibModalInstance, entity, PlayerCount) {
        var vm = this;

        vm.playerCount = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            PlayerCount.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
