import { Label } from './ui/label';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { RadioGroup, RadioGroupItem } from './ui/radio-group';
import { Switch } from './ui/switch';
import { Card, CardContent } from './ui/card';

interface GeneralInfo {
  name: string;
  code: string;
  description: string;
  category: string;
  brand: string;
  supplier: string;
  gender: string;
  basePurchasePrice: string;
  baseSalePrice: string;
  minStock: string;
  active: boolean;
}

interface GeneralInfoTabProps {
  data: GeneralInfo;
  onChange: (field: string, value: any) => void;
}

export function GeneralInfoTab({ data, onChange }: GeneralInfoTabProps) {
  return (
    <Card className="border-0 shadow-md">
      <CardContent className="p-6">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Columna Izquierda */}
          <div className="space-y-4">
            <div>
              <Label htmlFor="name" className="text-sm font-medium">
                Nombre del Producto *
              </Label>
              <Input
                id="name"
                value={data.name}
                onChange={(e) => onChange('name', e.target.value)}
                placeholder="Ej: Zapatilla Deportiva Running Pro"
                className="mt-1.5 text-base"
              />
            </div>

            <div>
              <Label htmlFor="code" className="text-sm font-medium">
                Código del Modelo *
              </Label>
              <Input
                id="code"
                value={data.code}
                onChange={(e) => onChange('code', e.target.value)}
                placeholder="Ej: ZD-RUN-001"
                className="mt-1.5"
              />
            </div>

            <div>
              <Label htmlFor="description" className="text-sm font-medium">
                Descripción
              </Label>
              <Textarea
                id="description"
                value={data.description}
                onChange={(e) => onChange('description', e.target.value)}
                placeholder="Descripción detallada del producto..."
                className="mt-1.5 min-h-[120px]"
              />
            </div>
          </div>

          {/* Columna Derecha */}
          <div className="space-y-4">
            <div>
              <Label htmlFor="category" className="text-sm font-medium">
                Categoría *
              </Label>
              <Select value={data.category} onValueChange={(val) => onChange('category', val)}>
                <SelectTrigger id="category" className="mt-1.5">
                  <SelectValue placeholder="Seleccionar categoría" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="deportivo">Calzado Deportivo</SelectItem>
                  <SelectItem value="casual">Calzado Casual</SelectItem>
                  <SelectItem value="formal">Calzado Formal</SelectItem>
                  <SelectItem value="infantil">Calzado Infantil</SelectItem>
                  <SelectItem value="sandalias">Sandalias</SelectItem>
                  <SelectItem value="botas">Botas</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="brand" className="text-sm font-medium">
                Marca *
              </Label>
              <Select value={data.brand} onValueChange={(val) => onChange('brand', val)}>
                <SelectTrigger id="brand" className="mt-1.5">
                  <SelectValue placeholder="Seleccionar marca" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="adidas">Adidas</SelectItem>
                  <SelectItem value="nike">Nike</SelectItem>
                  <SelectItem value="puma">Puma</SelectItem>
                  <SelectItem value="reebok">Reebok</SelectItem>
                  <SelectItem value="converse">Converse</SelectItem>
                  <SelectItem value="vans">Vans</SelectItem>
                  <SelectItem value="new-balance">New Balance</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="supplier" className="text-sm font-medium">
                Proveedor Principal
              </Label>
              <Select value={data.supplier} onValueChange={(val) => onChange('supplier', val)}>
                <SelectTrigger id="supplier" className="mt-1.5">
                  <SelectValue placeholder="Seleccionar proveedor" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="proveedor-1">Distribuidora SportWear S.A.</SelectItem>
                  <SelectItem value="proveedor-2">Importadora Global Shoes</SelectItem>
                  <SelectItem value="proveedor-3">Calzados Premium Ltda</SelectItem>
                  <SelectItem value="proveedor-4">Mayorista Deportes XYZ</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label className="text-sm font-medium mb-3 block">
                Género *
              </Label>
              <RadioGroup 
                value={data.gender} 
                onValueChange={(val) => onChange('gender', val)}
                className="flex flex-wrap gap-4"
              >
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="mujer" id="mujer" />
                  <Label htmlFor="mujer" className="font-normal cursor-pointer">MUJER</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="hombre" id="hombre" />
                  <Label htmlFor="hombre" className="font-normal cursor-pointer">HOMBRE</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="nino" id="nino" />
                  <Label htmlFor="nino" className="font-normal cursor-pointer">NIÑO</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <RadioGroupItem value="unisex" id="unisex" />
                  <Label htmlFor="unisex" className="font-normal cursor-pointer">UNISEX</Label>
                </div>
              </RadioGroup>
            </div>
          </div>
        </div>

        {/* Fila Inferior */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mt-6 pt-6 border-t border-gray-200">
          <div>
            <Label htmlFor="basePurchasePrice" className="text-sm font-medium">
              Precio Compra Base
            </Label>
            <div className="relative mt-1.5">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <Input
                id="basePurchasePrice"
                type="number"
                value={data.basePurchasePrice}
                onChange={(e) => onChange('basePurchasePrice', e.target.value)}
                placeholder="0.00"
                className="pl-7"
              />
            </div>
          </div>

          <div>
            <Label htmlFor="baseSalePrice" className="text-sm font-medium">
              Precio Venta Base
            </Label>
            <div className="relative mt-1.5">
              <span className="absolute left-3 top-2.5 text-gray-500">$</span>
              <Input
                id="baseSalePrice"
                type="number"
                value={data.baseSalePrice}
                onChange={(e) => onChange('baseSalePrice', e.target.value)}
                placeholder="0.00"
                className="pl-7"
              />
            </div>
          </div>

          <div>
            <Label htmlFor="minStock" className="text-sm font-medium">
              Stock Mínimo
            </Label>
            <Input
              id="minStock"
              type="number"
              value={data.minStock}
              onChange={(e) => onChange('minStock', e.target.value)}
              placeholder="0"
              className="mt-1.5"
            />
          </div>

          <div>
            <Label htmlFor="active" className="text-sm font-medium block mb-2">
              Estado Activo
            </Label>
            <div className="flex items-center space-x-2 mt-1.5">
              <Switch
                id="active"
                checked={data.active}
                onCheckedChange={(checked) => onChange('active', checked)}
              />
              <span className="text-sm text-gray-600">
                {data.active ? 'Activo' : 'Inactivo'}
              </span>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
