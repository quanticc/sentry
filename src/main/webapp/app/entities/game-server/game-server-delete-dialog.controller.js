(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('GameServerDeleteController',GameServerDeleteController);

    GameServerDeleteController.$inject = ['$uibModalInstance', 'entity', 'GameServer'];

    function GameServerDeleteController($uibModalInstance, entity, GameServer) {
        var vm = this;

        vm.gameServer = entity;
        vm.clear = clear;
        vm.confirmDelete = confirmDelete;

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function confirmDelete (id) {
            GameServer.delete({id: id},
                function () {
                    $uibModalInstance.close(true);
                });
        }
    }
})();
